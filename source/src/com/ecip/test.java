package com.ecip;

import java.util.Date;

public class test {
private Date d1=new Date();

public void clear()
{
	 this.clearProp(d1);
}
private void clearProp(Date d12) {
	if(d12!=null)
	{
		d12=null;
		System.out.println("cleared ");
	}else
	{
		System.out.println("already cleared ");
	}
	
	
}
public static void main(String[] args)
{
	test t1=new test();
	t1.clear();
	t1.clear();
}
}
