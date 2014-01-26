package com.micmiu.websocket.speedtest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;


/**
 * wbsocket 实现测速测速
 * @author <a href="http://www.micmiu.com">Michael</a>
 * @create Jan 26, 2014 4:25:17 PM 
 * @version 1.0
 * @logs
	<table cellPadding="1" cellSpacing="1" width="300">
	<thead style="font-weight:bold;background-color:#2FABE9">
		<tr>	<td>Date</td><td>Author</td><td>Version</td><td>Comments</td>	</tr>
	</thead>
	<tbody style="background-color:#b5cfd2">
		<tr>	<td>Jan 26, 2014</td><td><a href="http://www.micmiu.com">Michael</a></td><td>1.0</td><td>Create</td>	</tr>
	</tbody>
	</table>
 */
public class SpeedTestServlet extends WebSocketServlet {

	private static final long serialVersionUID = -7711109997662470450L;

	private static final String MSG_PROCESS = " process percent = %d %%";
	private static final String MSG_PROCESS_UP = "up " + MSG_PROCESS;
	private static final String MSG_PROCESS_DOWN = "down " + MSG_PROCESS;
	private static final String MSG_AVG = " avg speed = %d bps";
	private static final String MSG_AVG_UP = "up " + MSG_AVG;
	private static final String MSG_AVG_DOWN = "down " + MSG_AVG;

	private static final String MSG_DOWN_INFO = "hxws={'type':'down','handler':'info','speed':%d,'process':%d}";
	private static final String MSG_UP_INFO = "hxws={'type':'up','handler':'info','speed':%d,'process':%d}";
	private static final String MSG_UP_START = "hxws={'type':'up','handler':'sendpkg','pkglen':%d,'status':'start'}";
	private static final String MSG_UP_SEND = "hxws={'type':'up','handler':'sendpkg','pkglen':%d}";

	private final Set<SpeedMessageInbound> connections = new CopyOnWriteArraySet<SpeedMessageInbound>();

	@Override
	protected StreamInbound createWebSocketInbound(String subProtocol,
			HttpServletRequest request) {
		String sid = request.getSession().getId();
		System.out.println(sid);
		return new SpeedMessageInbound(sid);
	}

	private final class SpeedMessageInbound extends MessageInbound {

		private String sid;

		private SpeedMessageInbound(String sid) {
			super();
			this.sid = sid;
		}

		private int pkglen = DataUtil.DEF_PKG_LEN;
		private int testTimes = DataUtil.DEF_TEST_TIMES;

		private int upPkgCountTotal = 0, upPkgCountLast = 0;
		private int upPkgSizeTotal = 0, upPkgSizeLast = 0;
		private long upTimeStart = 0, upTimeLast = 0, upTimePre = 0;
		private long percent = 0;

		@Override
		protected void onOpen(WsOutbound outbound) {
			try {
				connections.add(this);
				System.out.println(">> conn client size = "
						+ connections.size());
				String message = String.format(
						"# <%s> has joined,current client size = %d", sid,
						connections.size());
				System.out.println(message);
				this.getWsOutbound().writeTextMessage(CharBuffer.wrap(message));
			} catch (IOException e) {
				//
			}
		}

		@Override
		protected void onClose(int status) {
			try {
				connections.remove(this);
				String message = String.format("# <%s> has disconnected.", sid);
				System.out.println(message);
				this.getWsOutbound().writeTextMessage(CharBuffer.wrap(message));
			} catch (IOException e) {
				//
			}
		}

		@Override
		protected void onBinaryMessage(ByteBuffer byteBuf) throws IOException {
			if (upTimeStart == 0) {
				upTimeStart = System.currentTimeMillis();
				upTimePre = upTimeLast = upTimeStart;
			}
			long currTime = System.currentTimeMillis();
			int size = byteBuf.limit();
			upPkgCountTotal++;
			upPkgSizeTotal += byteBuf.limit();

			long currPer = (currTime - upTimeStart) * 100 / 1000 / testTimes;
			System.out.println(currPer);
			if (currPer >= 100) {
				long currSpeed = upPkgCountTotal * pkglen * 1000
						/ (currTime - upTimeStart);
				this.getWsOutbound().writeTextMessage(
						CharBuffer.wrap(String.format(MSG_UP_INFO, currSpeed,
								100)));
			} else {
				if (currPer > percent) {
					long currSpeed = (upPkgCountTotal - upPkgCountLast)
							* pkglen * 1000 / (currTime - upTimeLast);
					upTimeLast = currTime;
					upPkgCountLast = upPkgCountTotal;
					percent = currPer;
					this.getWsOutbound().writeTextMessage(
							CharBuffer.wrap(String.format(MSG_UP_INFO,
									currSpeed, currPer)));
				}
				if (size == pkglen) {
				this.getWsOutbound().writeTextMessage(
						CharBuffer.wrap(String.format(MSG_UP_SEND, pkglen)));
				}
			}
			System.out.println(String.format(
					"received binary pkg count = %d , size = %d ",
					upPkgCountTotal, upPkgSizeTotal));

		}

		@Override
		protected void onTextMessage(CharBuffer charBuf) throws IOException {
			System.out.println(MessageFormat.format(
					"收到来自客户端 <{0}> 的消息 >>: [{1}]", sid, charBuf.toString()));

			String msg = charBuf.toString();
			WsOutbound out = this.getWsOutbound();
			// testspeed=type:download,status:start,times:30,pkglen:1400
			if (msg.startsWith("testspeed")) {
				Map<String, String> params = new HashMap<String, String>();
				if (msg.indexOf('=') > -1 && msg.indexOf(',') > -1) {
					for (String kv : msg.split("=")[1].split(",")) {
						String[] arr = kv.split(":");
						if (arr.length > 1) {
							params.put(arr[0], arr[1]);
						}
					}
				} else {
					params.put("type", "download");
					params.put("status", "start");
				}

				String type = params.get("type");

				if ("download".equals(type)) {
					handler4Download(params);
				} else if ("upload".equals(type)) {
					init4up();
					out.writeTextMessage(CharBuffer.wrap(String.format(
							MSG_UP_START, pkglen)));
				} else {
					out.writeTextMessage(CharBuffer
							.wrap("Sorry, unsupport type=" + type));
				}

			} else if ("calcupspeed".equals(msg)) {
				System.out.println(upTimePre + " - " + upTimeLast);
				System.out.println(upPkgSizeTotal + " - " + upPkgSizeLast);
				long usetimes = upTimeLast - upTimePre;
				upTimePre = upTimeLast;
				long speed = (upPkgSizeTotal - upPkgSizeLast) * 1000 / usetimes;
				upPkgSizeLast = upPkgSizeTotal;
				this.getWsOutbound().writeTextMessage(
						CharBuffer.wrap(String.format(MSG_AVG_UP, speed)));

			} else if ("testupend".equals(msg)) {
				long usetimes = upTimeLast - upTimeStart;
				long speed = upPkgSizeTotal * 1000 / usetimes;
				this.getWsOutbound().writeTextMessage(
						CharBuffer.wrap(String.format(MSG_AVG_UP, speed)));

			} else if ("testbinary".equals(msg)) {
				out.writeBinaryMessage(ByteBuffer.wrap(DataUtil
						.randomByteData(8)));

			} else {
				CharBuffer buffer = CharBuffer.wrap("server recevied message: "
						+ charBuf);
				out.writeTextMessage(buffer);
			}

			out.flush();

		}

		private void init4up() {
			upPkgCountTotal = upPkgCountLast = 0;
			upPkgSizeTotal = upPkgSizeLast = 0;
			upTimeStart = upTimeLast = 0;
		}

		private void handler4Download(Map<String, String> params)
				throws IOException {
			WsOutbound out = this.getWsOutbound();
			String status = params.get("status");
			pkglen = DataUtil.parsePkgLen(params.get("pkglen"));
			testTimes = DataUtil.parseTimes(params.get("times"));
			long pkgCount = 0, pkgCountLast = 0, percent = 0;
			if ("start".equals(status)) {
				byte[] data = DataUtil.randomByteData(pkglen);
				long start = System.currentTimeMillis();
				long lastTime = start, currTime = start;
				while ((currTime - start) < testTimes * 1000) {
					out.writeBinaryMessage(ByteBuffer.wrap(data));
					pkgCount++;
					currTime = System.currentTimeMillis();
					long currPer = (currTime - start) * 100 / 1000 / testTimes;
					if (currPer >= 100) {
						break;
					}
					if (currPer > percent) {
						long currSpeed = (pkgCount - pkgCountLast) * pkglen
								* 1000 / (currTime - lastTime);
						out.writeTextMessage(CharBuffer.wrap(String.format(
								MSG_DOWN_INFO, currSpeed, currPer)));
						percent = currPer;
						pkgCountLast = pkgCount;
						lastTime = currTime;
					}
				}
				long usetimes = (currTime - start) / 1000;
				long avgspeed = pkgCount * pkglen / usetimes;
				System.out.println("download pkg count = " + pkgCount
						+ " times = " + usetimes);
				System.out.println("download avg speed = " + avgspeed);
				out.writeTextMessage(CharBuffer.wrap(String.format(
						MSG_DOWN_INFO, avgspeed, 100)));
				String next = params.get("nextstep");
				if (null != next && "upload".equals(next)) {
					init4up();
					out.writeTextMessage(CharBuffer.wrap(String.format(
							MSG_UP_SEND, pkglen)));
				}
			} else {
				out.writeTextMessage(CharBuffer.wrap("Unsupport status="
						+ status));
			}
		}

		private void broadcast(String message) {
			for (SpeedMessageInbound msgInbound : connections) {
				try {
					CharBuffer buffer = CharBuffer.wrap(message);
					msgInbound.getWsOutbound().writeTextMessage(buffer);
				} catch (IOException ignore) {
					// Ignore
				}
			}
		}

	}
}
