package com.micmiu.websocket.speedtest;


/**
 * 数据包处理
 * @author <a href="http://www.micmiu.com">Michael</a>
 * @create Jan 26, 2014 4:51:34 PM 
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
public class DataUtil {

	public static final int DEF_PKG_LEN = 1400;
	public static final int DEF_TEST_TIMES = 30;

	public static byte[] randomByteData(int length) {
		byte[] datas = new byte[length];
		for (int i = 0; i < datas.length; i++) {
			datas[i] = 'A';
		}
		return datas;
	}

	public static int parsePkgLen(String value) {
		try {
			if (null == value) {
				return DEF_PKG_LEN;
			}
			return Integer.parseInt(value);
		} catch (Exception e) {
			return DEF_PKG_LEN;
		}
	}

	public static int parseTimes(String value) {
		try {
			if (null == value) {
				return DEF_TEST_TIMES;
			}
			return Integer.parseInt(value);
		} catch (Exception e) {
			return DEF_TEST_TIMES;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
