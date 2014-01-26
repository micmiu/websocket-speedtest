function trim(text) {
		return (text+"").replace(/(^\s*)|(\s*$)/g, "");
	}
function toDecimal(x) {
    var f = parseFloat(x);  
    if (isNaN(f)) {  
        return;  
    } else{
    	return Math.round(x*100)/100;  
    }
}  
function unitConver(unit){
	var index=0;
	var moveidex=0;
	var limit=trim(unit).toLowerCase();//转换为小写	
	if(limit.indexOf('b')==-1){ //如果无单位,加单位递归转换
		limit=limit+"b";
	}
	var reCat=/[0-9]*[a-z]b/;
	if(!reCat.test(limit)&&limit.indexOf('b')!=-1&&limit.length>4){ //如果单位是b,转换为kb加单位递归
		limit=limit.substring(0,limit.indexOf('b')); //去除单位,转换为数字格式
		limit=toDecimal((limit/1024))+'kb'; //换算舍入加单位		
	}
	var array=new Array('kb','mb','gb','tb','pt');
	for(var i=0;i<array.length;i++){ //记录所在的位置
		if(limit.indexOf(array[i])!=-1){
			index=i;
			break;
		}
	}
	limit=limit.substring(0,(limit.length-2)); //得到纯数字	
	for(var i=0;i<array.length-index;i++){
		limit=toDecimal(limit/1024); //每次缩小一个单位		
		if(limit<1024){ //不行进行下一次的换算单位
			moveidex=i+1;	//记录做了几次除法运算/至少执行一次
			break;
		}
	}
	limit=limit+array[index+moveidex]; //根据原位置和移动后的位置加入单位
	return limit.toUpperCase();
}