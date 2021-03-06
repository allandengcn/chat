/**
 * @Title: ServerThread.java
 * @Package cn.allandeng.server
 * @Description: TODO
 * Copyright: Copyright (c) 2019
 * 
 * @author 邓依伦
 * @date 2019年10月31日 下午1:19:23
 * @version V1.0
 */
package cn.allandeng.server;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import cn.allandeng.server.data.Dml;
import cn.allandeng.server.data.Query;
import cn.allandeng.server.model.ClientsMap;
import cn.allandeng.common.Massage;
import cn.allandeng.common.MassageType;
import cn.allandeng.common.UserInfo;

/**
  * @ClassName: ServerThread
  * @Description: TODO
  * @author 邓依伦-Allan
  * @date 2019年10月31日 下午1:19:23
  *
  */
public class ServerThread extends Thread{
	private Socket socket ;
	ObjectInputStream ois = null;
	ObjectOutputStream oos = null;
	
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	//构造器 用来获取Socket对象
	/**
	  * 创建一个新的实例 ServerThread. 
	  */
	public ServerThread(Socket socket) {
		this.socket = socket ;
	}
	
	
	@Override
	public void run() {
		try {
			//获取Socket的对象输入输出流
			//System.out.println("已启动一个服务线程");
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
			//System.out.println("创建输入输出流完成");
			//接收到的massage对象
			Massage buffer = null;
			
			//已对象为单位收发数据
			while (true) {
				buffer = (Massage)ois.readObject();
				//System.out.println(buffer);
				//System.out.println(buffer.getText());
				//System.out.println("收到消息。");
				switch (buffer.getType()) {
				case ONLINE:
					//System.out.println("收到上线消息");
					serverOnline(buffer , CreateSocket.clients ,oos , CreateSocket.userNicknames);
					buffer = null;
					break;
				case OFFLINE:
					serverOffline(buffer);
					buffer = null;
					break;
				case TEXT:
					//System.out.println(buffer+buffer.getText());
					serverForwardMassage(buffer , CreateSocket.clients ,CreateSocket.userNicknames);
					buffer = null;
					break;
				case QUERY:
					//处理客户端发过来的指令
					//System.out.println("收到请求");
					answerQuery(buffer , CreateSocket.clients ,CreateSocket.userNicknames);
					buffer = null;
					break;
				default:
					break;
				}
			}
			
			//System.out.println("处理线程结束");
			
		} catch (Exception e) {
			// 如果发生异常说明这个客户端下线了
			//e.printStackTrace();
			System.out.println("客户端下线：" + CreateSocket.clients.getKeyByValue(oos));
			serverClose();
		}
	}


	/**
	  * @Title: answerQuery
	  * @Description: TODO
	  * @param @param buffer
	  * @param @param clients
	  * @param @param userNicknames    设定文件
	  * @return void    返回类型
	  * @throws
	  */
	private void answerQuery(Massage buffer, ClientsMap<Integer, ObjectOutputStream> clients,
			ClientsMap<Integer, String> userNicknames) {
		String command = buffer.getText().split(" ")[0];
		switch (command) {
		case "queryonline":
			int uid = buffer.getSendUID();
			Massage answerMassage = new Massage(MassageType.ANSWER, 0, uid);
			answerMassage.setText(buffer.getText());
			//实现深拷贝，如果使用浅拷贝，只拷贝引用则无法发送更新后的map
			Map<Integer, String> m = new HashMap<Integer, String>();
			m.putAll(CreateSocket.userNicknames.map);
			answerMassage.setSendObject(m);
			//发送查询结果
			if (clients.map.containsKey(uid)) {
				try {
					clients.map.get(uid).writeObject(answerMassage);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else {
				if (GlobalVariable.showChatMassage) {
					System.out.println("目标用户：" + uid + "不在线");
				}
				
			}
			break;
		case "changenickname":
			int id = Integer.parseInt(buffer.getText().split(" ")[1]);
			String nickname = buffer.getText().split(" ")[2];
			
			Massage answerMassage1 = new Massage(MassageType.ANSWER, 0, id);
			Dml.updataNickName(id, nickname);
			CreateSocket.userNicknames.removeByValue(id);
			CreateSocket.userNicknames.put(id, nickname);
			answerMassage1.setText("changenickname");
			
			try {
				clients.map.get(id).writeObject(answerMassage1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case "changepassword":
			int id1 = Integer.parseInt(buffer.getText().split(" ")[1]);
			String oldpwd = buffer.getText().split(" ")[2];
			String newpwd = buffer.getText().split(" ")[3];
			//System.out.println(buffer.getText());
			Massage answerMassage11 = new Massage(MassageType.ANSWER, 0, id1);
			if (new Query().isValidLoginInfo(id1, oldpwd)) {
				Dml.updataPassword(id1, newpwd);
				answerMassage11.setText("changepassword");
			}else {
				answerMassage11.setText("error");
			}
			try {
				clients.map.get(id1).writeObject(answerMassage11);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
		
	}


	/**
	 * @param userNicknames 
	  * @Title: serverForwardMassage
	  * @Description: 转发用户消息
	  * @param @param buffer	需要处理的消息
	  * @param @param clients	在线用户列表
	  * @return void    返回类型
	  * @throws
	  */
	private void serverForwardMassage(Massage buffer, ClientsMap<Integer, ObjectOutputStream> clients, ClientsMap<Integer, String> userNicknames) {
		int uid = buffer.getSendUID();
		int receiveuid =buffer.getReceiveUID();
		if (GlobalVariable.showChatMassage) {
			System.out.println("---" + df.format(new Date()) + "---");
		}
		
		if(receiveuid == 0 ) {
			//群发
			if (GlobalVariable.showChatMassage) {
				System.out.println(uid + "   "+ userNicknames.map.get(uid)+"对所有人说：");
			}
			
			for (ObjectOutputStream oos:clients.valueSet()) {
				try {
					oos.writeObject(buffer);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}else {
			//点对点发送
			if (clients.map.containsKey(receiveuid)) {
				if (GlobalVariable.showChatMassage) {
					System.out.println(uid + "   "+userNicknames.map.get(uid)+ "对" + receiveuid + "   " +userNicknames.map.get(receiveuid)+"说：");
				}
				
				try {
					clients.map.get(receiveuid).writeObject(buffer);
					clients.map.get(uid).writeObject(buffer);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else {
				if (GlobalVariable.showChatMassage) {
					System.out.println("目标用户：" + receiveuid + "不在线");
				}
				
			}
		}
		if (GlobalVariable.showChatMassage) {
			System.out.println(buffer.getText());
			System.out.println(new String(new char[25]).replace("\0", "-"));
		}
		
	}


	/**
	  * @Title: serverOffline
	  * @Description: 处理用户下线
	  * @param @param buffer	需要处理的消息
	  * @return void    返回类型
	  * @throws
	  */
	private void serverOffline(Massage buffer) {
		int uid = buffer.getSendUID();
		System.out.println("用户" + uid +"下线了！");
		serverClose();
	}


	/**
	 * @param userNicknames 
	 * @param clients 
	 * @param oos 
	  * @Title: serverOnline
	  * @Description: 处理用户上线
	  * @param @param buffer	需要处理的消息
	  * @return void    返回类型
	  * @throws
	  */
	private void serverOnline(Massage buffer, ClientsMap<Integer, ObjectOutputStream> clients, ObjectOutputStream oos, ClientsMap<Integer, String> userNicknames) {
		//判断当前用户是否在线
		int uid = buffer.getSendUID();
		String password =buffer.getUserDetailInfo().getPassword();
		String nickName ;
		if(validLogin(uid,password)) {
			
			nickName = queryNickname(uid);
			
			if (clients.map.containsKey(uid)) {
				//如果包含则更新输出流
				System.out.println("用户" + uid +"重新上线了！");
				clients.map.remove(uid);
			}else {
				//若不包含则添加输出流
				System.out.println("用户" + uid +"上线了！");
			}
			clients.put(uid, oos);
			userNicknames.put(uid, nickName);
			//返回上线结果
			Massage loginMassage = new Massage(MassageType.ONLINE_SUCCESS,0, uid);
			loginMassage.setUserInfo(new UserInfo(nickName));
			try {
				oos.writeObject(loginMassage);
			} catch (IOException e) {
				
				e.printStackTrace();
				System.out.println("用户异常下线：" + uid);
				//serverClose();
			}
		}else {
			try {
				oos.writeObject(new Massage(MassageType.ONLINE_FAIL, 0, uid));
			} catch (IOException e) {
				
				e.printStackTrace();
				System.out.println("用户异常下线：" + uid);
				//serverClose();
			}
		}
		
		
		
	}


	/**
	  * @Title: queryNickname
	  * @Description: 查询昵称
	  * @param @param uid
	  * @param @return    设定文件
	  * @return String    返回类型
	  * @throws
	  */
	private String queryNickname(int uid) {
		return new Query().getNickname(uid);
	}


	/**
	  * @Title: validLogin
	  * @Description: 核实用户名和密码,
	  * @param @param uid
	  * @param @param password
	  * @param @return    设定文件
	  * @return boolean    返回类型
	  * @throws
	  */
	private boolean validLogin(int uid, String password) {
		
		if (new Query().isValidLoginInfo(uid, password)) {
			return true;
		}else {
			return false;
		}
		
	}


	/**
	  * @Title: serverClose
	  * @Description: 关闭连接
	  * @param     设定文件
	  * @return void    返回类型
	  * @throws
	  */
	private void serverClose() {
		//1.从map中删除本数据 2.关闭ois oos 3.关闭socket
		CreateSocket.clients.removeByValue(oos);
		try {
			if (ois != null) {
				ois.close();
			}
			if (oos != null) {
				oos.close();
			}
			if (socket != null) {
				socket.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
