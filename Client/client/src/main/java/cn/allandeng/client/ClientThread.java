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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import cn.allandeng.common.Massage;
import cn.allandeng.common.MassageType;
import cn.allandeng.common.UserDetailInfo;
import cn.allandeng.common.UserInfo;

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
	private static int uid ;
	private static String password ;
	private static String nickName ;
	
	//在线 用户表
	public static Map<Integer, String> userOnline = new HashMap<>() ;
	
	private static ObjectInputStream ois = null;
	private static ObjectOutputStream oos = null;
	//private Scanner keyin = null;
	private BufferedReader keyin = null;
	private InputStream in = new FilterInputStream(System.in){
        @Override
        public void close() throws IOException {
            //do nothing
        }
	};
	
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
		
		//输入用户登陆信息
		Scanner sc = new Scanner(in);
		System.out.println("请输入用户ID：");
		uid = sc.nextInt();
		System.out.println("请输入用户密码：");
		sc.nextLine();
		password = sc.nextLine();
//		System.out.println("请输入用户昵称：");
//		nickName = sc.nextLine();
		sc.close();
		
		//创建用户信息对象
		ClientMain.userDetailInfo = new UserDetailInfo();
		ClientMain.userDetailInfo.setPassword(password);
		
		//创建登陆用massage
		Massage loginMassage = new Massage(MassageType.ONLINE, uid, 0);
		loginMassage.setUserDetailInfo(ClientMain.userDetailInfo);
		try {
			while(true) {
				try {
					oos.writeObject(loginMassage);
					//System.out.println("发送成功");
					Massage respond = null;
					respond = (Massage)ois.readObject();
				
					if (respond.getType() == MassageType.ONLINE_SUCCESS) {
						nickName = respond.getUserInfo().getNickName();
						ClientMain.userInfo = new UserInfo(nickName);
						System.out.println("登陆成功，当前用户ID为：" + uid+"，昵称为："+ nickName);
						new ListenMassage(ois).start();
						new Timer().schedule(new QueryOnlineUser() , 1000);
						break;
					}else {
						if (respond.getType() == MassageType.ONLINE_FAIL) {
							System.out.println("登陆信息错误");
							//输入用户登陆信息
							Scanner sc1 = new Scanner(in);
							System.out.println("请输入用户ID：");
							uid = sc1.nextInt();
							System.out.println("请输入用户密码：");
							sc1.nextLine();
							password = sc1.nextLine();
//							System.out.println("请输入用户昵称：");
//							nickName = sc.nextLine();
							sc1.close();
							//新的登陆对象
							loginMassage = new Massage(MassageType.ONLINE, uid, 0);
							ClientMain.userDetailInfo = new UserDetailInfo();
							ClientMain.userDetailInfo.setPassword(password);
							loginMassage.setUserDetailInfo(ClientMain.userDetailInfo);
						}else {
							System.out.println("登陆失败，3秒后重试");
							sleep(3000);
						}
						
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
		
			//System.out.println("出循环了");
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
		keyin = new BufferedReader(new InputStreamReader(in));
		
		String tip = "";
		
		try {

			System.out.println("请输入聊天内容");
			//while(true) {
			//System.out.println(keyin.hashCode());
			while((tip = keyin.readLine()) != null) {
				procesCommand(tip);
				
			}
		} catch (Exception e) {
			System.out.println("出现故障，系统退出");
			e.printStackTrace();
			//closeClient();
			System.exit(1);
		}
		

	}
	
	/**
	 * @throws IOException 
	 * @param sendMassage 
	  * @Title: procesCommand
	  * @Description: TODO
	  * @param @param tip    设定文件
	  * @return void    返回类型
	  * @throws
	  */
	private boolean procesCommand(String tip) throws IOException {
		Massage sendMassage = new Massage(MassageType.TEXT, uid, 0);
		sendMassage.setUserInfo(ClientMain.userInfo);
		//System.out.println("收到输入:" + tip);
		//tip = keyin.nextLine();
		if (tip.length()>0) {
			//System.out.println("收到输入:" + tip);
			//判断是否为帮助
			if(tip.equals(":help")) {
				displayFile("config/help.txt");
				return true ;
			}
			//判断是否为关于
			if(tip.equals(":about")) {
				displayFile("config/about.txt");
				return true ;
			}
			//判断是否为下线消息
			if(tip.equals(":quit")) {
				sendMassage.setType(MassageType.OFFLINE);
				oos.writeObject(sendMassage);
				closeClient();
				System.exit(1);
				return true ;
			}
			//判断是否为显示在线用户
			if(tip.equals(":online")) {
				int i = 1  ;
				System.out.println("----在线用户列表----");
				for(Integer id:ClientThread.userOnline.keySet()) {
					System.out.println("第"+i+"个用户    ID:" + id+"   昵称："+ClientThread.userOnline.get(id));
					i++;
				}
				return false ;
			}
			//判断是不是修改昵称
			if (tip.split(" ")[0].equals(":changenickname")) {
				String[] s =tip.split(" ");
				int uid = Integer.parseInt(s[1]);
				String nickName = s[2];
				sendMassage.setType(MassageType.QUERY);
				sendMassage.setText("changenickname " + uid + " " +nickName);
				//System.out.println("changenickname " + uid + " " +nickName);
				oos.writeObject(sendMassage);
				return true ;
			}
			//判断是不是修改密码
			if (tip.split(" ")[0].equals(":changepassword")) {
				String[] s =tip.split(" ");
				String oldpassword = s[2];
				String newpassword = s[3];
				sendMassage.setType(MassageType.QUERY);
				sendMassage.setText("changepassword " + uid + " " +oldpassword + " " +newpassword);
				oos.writeObject(sendMassage);
				return true ;
			}
			//判断是群发还是单发消息
			//存在@开头并且有：则为群发
			if(tip.charAt(0) == '@' && tip.indexOf(':')>0) {
				//System.out.println("单发消息");
				
				sendMassage.setType(MassageType.TEXT);
				sendMassage.setReceiveUID(
						Integer.parseInt(tip.substring(1,tip.indexOf(':')))
						);
				sendMassage.setText(tip.substring(tip.indexOf(':')+1, tip.length()));
				oos.writeObject(sendMassage);
				return true ;
				//System.out.println(sendMassage.getText());
			}else {
				//System.out.println("群发消息");
				sendMassage.setType(MassageType.TEXT);
				sendMassage.setReceiveUID(0);
				sendMassage.setText(tip);
				oos.writeObject(sendMassage);
				return true ;
				//System.out.println(sendMassage);
			}
			
		}
		return false;
				
	}

	/**
	  * @Title: displayFile
	  * @Description: 显示文件
	  * @param @param fileAddress    设定文件
	  * @return void    返回类型
	  * @throws
	  */
	private void displayFile(String fileAddress) {
		String encoding = "utf8";
		String file = fileAddress;
		
		try {
			BufferedReader fr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(file)) , encoding));
			String lineTxt = "";
			while ((lineTxt = fr.readLine()) != null)
            {
                System.out.println(lineTxt);
            }
			fr.close();
						
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("找不到文件或无法打开文件");
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
		readAndSend();	
		
		
		
	}
	
	/**
	 * getter method
	 * @return the uid
	 */
	public static int getUid() {
		return uid;
	}
	
	/**
	 * getter method
	 * @return the oos
	 */
	public static ObjectOutputStream getOos() {
		return oos;
	}
}


/**
  * @ClassName: ListenMassage
  * @Description: 内部类，用来监听服务器发回来的消息
  * @author 邓依伦-Allan
  * @date 2019年11月1日 下午5:10:14
  *
  */
class ListenMassage extends Thread{
	private ObjectInputStream ois = null;
	private Massage buffer = null;
	
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/**
	  * 创建一个新的实例 ListenMassage. 
	  * <p>Title: </p>
	  * <p>Description:  获得输入流</p>
	  */
	public ListenMassage(ObjectInputStream ois) {
		this.ois = ois;
	}
	
	/*
	  * <p>Title: run</p>
	  * <p>Description: </p>
	  * @see java.lang.Thread#run()
	  */
	@Override
	public void run(){
		//死循环
		while (true) {
			try {
				buffer = (Massage)ois.readObject();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}  catch (SocketException e) {
				// TODO: handle exception
				System.out.println("网络错误，服务器已下线？");
				break;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
			//System.out.println("收到服务器发的消息。");
			switch (buffer.getType()) {
			case TEXT:
				int uid = buffer.getSendUID();
				int receiveuid =buffer.getReceiveUID();
				String uidNickname = buffer.getUserInfo().getNickName();
				String receiveuidNickname = buffer.getUserInfo().getNickName();
				System.out.println("---" + df.format(new Date()) 
						+ "---");
				if(receiveuid == 0) {
					//群发
					
					if (uid == ClientThread.getUid() ) {
						System.out.println("我对所有人说：");
					}else {
						System.out.println(uid + "   "+ uidNickname+ "对所有人说：");
					}
				}else {
					//点对点发送
					if (uid == ClientThread.getUid() ) {
						System.out.println("我对"+ receiveuid + "   "+ receiveuidNickname+"说：");
					}else {
						System.out.println(uid + "   "+uidNickname+ "对我说：");
					}
					
					
				}
				System.out.println(buffer.getText());
				System.out.println(new String(new char[25]).replace("\0", "-"));
				buffer = null;
				break;
			case ANSWER:
				String ans = buffer.getText().split(" ")[0];
				switch (ans) {
				case "queryonline":
					//System.out.println("收到新在线用户表");
					Map<Integer, String> m = (Map<Integer, String>)buffer.getSendObject();
					ClientThread.userOnline = m ;
//					int i = 1  ;
//					for(Integer id:m.keySet()) {
//						System.out.println("第"+i+"个用户    ID:" + id+"   昵称："+m.get(id));
//						i++;
//					}
					break;
				case "changepassword":
					System.out.println("密码修改成功");
					break;
				case "changenickname":
					System.out.println("昵称修改成功");
					//修改昵称之后更新在线用户表
					
					break;
				case "error":
					System.out.println("请求错误");
					break;
				default:
					break;
				}
				break;
			default:
				
				break;
			}
			buffer = null;
		}
	}
}


class QueryOnlineUser extends TimerTask{
	@Override
	public void run() {
		//System.out.println("执行请求");
		Massage queryMassage = new Massage(MassageType.QUERY, ClientThread.getUid(),0  );
		queryMassage.setText("queryonline");
		try {
			ClientThread.getOos().writeObject(queryMassage);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
		new Timer().schedule(new QueryOnlineUser(), 10000);
	}
	
}

