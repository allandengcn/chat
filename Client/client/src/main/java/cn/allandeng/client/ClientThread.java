/**
 * @Title: ClientThread.java
 * @Package cn.allandeng.client
 * @Description: TODO
 * Copyright: Copyright (c) 2019
 * 
 * @author 邓依伦
 * @date 2019年10月31日 下午3:19:03
 * @version V1.0
 */
package cn.allandeng.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import cn.allandeng.client.model.Massage;
import cn.allandeng.client.model.MassageType;

/**
  * @ClassName: ClientThread
  * @Description: TODO
  * @author 邓依伦-Allan
  * @date 2019年10月31日 下午3:19:03
  *
  */
public class ClientThread extends Thread{
	private static final int SERVER_PORT = 6666;
	private static final String IP_ADDRESS = "127.0.0.1";
	private Socket socket;
	private int uid ;


	private ObjectInputStream ois = null;
	private ObjectOutputStream oos = null;
	private BufferedReader keyin = null;
	
	//初始化方法
	public void init() {
		boolean isConnected = false;
		do {
			try {
				//连接到服务器并获取输入输出流
				socket =new Socket(IP_ADDRESS, SERVER_PORT);
				oos= new ObjectOutputStream(socket.getOutputStream());
				ois= new ObjectInputStream(socket.getInputStream());
				System.out.println("已经连接上服务器");
				isConnected = true;
				
			} catch (UnknownHostException e) {
				System.out.println("服务器地址有误！");
				e.printStackTrace();
				System.exit(1);
			} catch (IOException e) {
				System.out.println("网络故障，3s后将重试！");
				//e.printStackTrace();
				isConnected = false;
				try {
					sleep(3000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		} while (!isConnected);
		
		Scanner sc = new Scanner(System.in);
		System.out.println("请输入用户ID：");
		uid = sc.nextInt();
		sc.close();
		try {
			while(true) {
				try {
					oos.writeObject(new Massage(MassageType.ONLINE, uid, 0));
					System.out.println("发送成功");
					Massage respond = null;
					while ((respond = (Massage)ois.readObject()) != null)
				
					if (respond.getType() == MassageType.ONLINE_SUCCESS) {
						System.out.println("登陆成功，当前用户为：" + uid);
						
						break;
					}else {
						System.out.println("登陆失败，3秒后重试");
						sleep(3000);
					}
				} catch (IOException e) {
					System.out.println("网络故障请重试！");
					e.printStackTrace();
					sleep(3000);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					sleep(3000);
				}
			}
		
			
		}catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	  * @Title: readAndSend
	  * @Description: 读取键盘输入并发送
	  * @param     设定文件
	  * @return void    返回类型
	  * @throws
	  */
	private void readAndSend() {
		//创建一个键盘输入流
		keyin =new BufferedReader(new InputStreamReader(System.in));
		String tip = "";
		Massage sendMassage = new Massage(MassageType.TEXT, uid, 0);
		try {
			while(true) {
				//如果读取到字符串
				if (( tip = keyin.readLine() )!= null) {
					
					//判断是否为下线消息
					if(tip.equals(":quit")) {
						sendMassage.setType(MassageType.OFFLINE);
						oos.writeObject(sendMassage);
						closeClient();
						System.exit(1);
						break;
					}
					//判断是群发还是单发消息
					//存在@开头并且有：则为群发
					if(tip.charAt(0) == '@' && tip.indexOf(':')>0) {
						sendMassage.setType(MassageType.TEXT);
						sendMassage.setReceiveUID(
								Integer.parseInt(tip.substring(1,tip.indexOf(':')-1))
								);
						sendMassage.setText(tip.substring(tip.indexOf(':')+1, tip.length()-1));
						oos.writeObject(sendMassage);
					}else {
						sendMassage.setType(MassageType.TEXT);
						sendMassage.setReceiveUID(0);
						sendMassage.setText(tip);
					}
				}
			}
		} catch (Exception e) {
			System.out.println("出现故障，系统退出");
			closeClient();
			System.exit(1);
		}
		

	}
	
	/**
	  * @Title: closeClient
	  * @Description: 关闭流
	  * @param     设定文件
	  * @return void    返回类型
	  * @throws
	  */
	private void closeClient() {
		try {
			if (keyin != null) {
				keyin.close();
			}
			if (oos != null) {
				oos.close();
			}
			if (ois != null) {
				ois.close();
			}
			if (socket != null) {
				socket.close();
			}
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
	}

	@Override
	public void run() {
		//初始化 
		init();
		//开始进行读写
		//readAndSend();	
		
	}
}
