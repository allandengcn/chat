/**
 * @Title: Massage.java
 * @Package cn.allandeng.server.model
 * @Description: TODO
 * Copyright: Copyright (c) 2019
 * 
 * @author 邓依伦
 * @date 2019年10月31日 下午12:47:41
 * @version V1.0
 */
package cn.allandeng.common;

import java.io.Serializable;

/**
  * @ClassName: Massage
  * @Description: 消息类
  * @author 邓依伦-Allan
  * @date 2019年10月31日 下午12:47:41
  *
  */
public class Massage implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private MassageType type ;
	private int sendUID ;
	private int receiveUID ;
	private String text ;
	private UserInfo userInfo;
	private UserDetailInfo userDetailInfo;
	
	private Object sendObject;
	
	/**
	  * 创建一个新的实例 Massage. 
	  * <p>Title: </p>
	  * <p>Description: </p>
	  */
	public Massage(MassageType type , int sendUID , int receiveUID) {
		this.type = type ; 
		this.sendUID = sendUID ;
		this.receiveUID = receiveUID ;
	}
	
	/**
	 * setter method
	 * @param type the type to set
	 */
	public void setType(MassageType type) {
		this.type = type;
	}
	
	/**
	 * setter method
	 * @param sendUID the sendUID to set
	 */
	public void setSendUID(int sendUID) {
		this.sendUID = sendUID;
	}
	
	/**
	 * setter method
	 * @param receiveUID the receiveUID to set
	 */
	public void setReceiveUID(int receiveUID) {
		this.receiveUID = receiveUID;
	}
	
	/**
	 * setter method
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}
	
	/**
	 * getter method
	 * @return the type
	 */
	public MassageType getType() {
		return type;
	}
	
	/**
	 * getter method
	 * @return the sendUID
	 */
	public int getSendUID() {
		return sendUID;
	}
	
	/**
	 * getter method
	 * @return the receiveUID
	 */
	public int getReceiveUID() {
		return receiveUID;
	}
	/**
	 * getter method
	 * @return the text
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * setter method
	 * @param userInfo the userInfo to set
	 */
	public void setUserInfo(UserInfo userInfo) {
		this.userInfo = userInfo;
	}
	/**
	 * setter method
	 * @param userDetailInfo the userDetailInfo to set
	 */
	public void setUserDetailInfo(UserDetailInfo userDetailInfo) {
		this.userDetailInfo = userDetailInfo;
	}
	/**
	 * getter method
	 * @return the userInfo
	 */
	public UserInfo getUserInfo() {
		return userInfo;
	}
	/**
	 * getter method
	 * @return the userDetailInfo
	 */
	public UserDetailInfo getUserDetailInfo() {
		return userDetailInfo;
	}
	
	/**
	 * setter method
	 * @param sendObject the sendObject to set
	 */
	public void setSendObject(Object sendObject) {
		this.sendObject = sendObject;
	}
	
	/**
	 * getter method
	 * @return the sendObject
	 */
	public Object getSendObject() {
		return sendObject;
	}
	
}
