package edu.mit.printAtMIT.model.touchstoneOld.internal;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

public class SerializableCookie implements Cookie, Serializable {
	private static final long serialVersionUID = 0L;
	
	private static final int COMMENT = 0x01;
	private static final int DOMAIN = 0x04;
	private static final int EXPIRY_DATE = 0x08;
	private static final int NAME = 0x10;
	private static final int PATH = 0x20;
	private static final int VALUE = 0x80;
	
	private transient Cookie _cookie = null;
	
	public SerializableCookie() {
		super();
	}
	
	public SerializableCookie(Cookie cookie)
	{
		super();
		_cookie = cookie;
	}

	@Override
	public String getComment() {
		if (this._cookie != null)
		{
			return this._cookie.getComment();
		}
		
		return null;
	}

	@Override
	public String getCommentURL() {
		if (this._cookie != null)
		{
			return this._cookie.getCommentURL();
		}
		
		return null;
	}

	@Override
	public String getDomain() {
		if (this._cookie != null)
		{
			return this._cookie.getDomain();
		}
		
		return null;
	}

	@Override
	public Date getExpiryDate() {
		if (this._cookie != null)
		{
			return this._cookie.getExpiryDate();
		}
		
		return null;
	}

	@Override
	public String getName() {
		if (this._cookie != null)
		{
			return this._cookie.getName();
		}
		
		return null;
	}

	@Override
	public String getPath() {
		if (this._cookie != null)
		{
			return this._cookie.getPath();
		}
		
		return null;
	}

	@Override
	public int[] getPorts() {
		if (this._cookie != null)
		{
			return this._cookie.getPorts();
		}
		
		return null;
	}

	@Override
	public String getValue() {
		if (this._cookie != null)
		{
			return this._cookie.getValue();
		}
		
		return null;
	}

	@Override
	public int getVersion() {
		if (this._cookie != null)
		{
			return this._cookie.getVersion();
		}
		
		return 0;
	}

	@Override
	public boolean isExpired(Date date) {
		if (this._cookie != null)
		{
			return this._cookie.isExpired(date);
		}
		
		return false;
	}

	@Override
	public boolean isPersistent() {
		if (this._cookie != null)
		{
			return this._cookie.isPersistent();
		}
		
		return false;
	}

	@Override
	public boolean isSecure() {
		if (this._cookie != null)
		{
			return this._cookie.isSecure();
		}
		
		return false;
	}
	
	private void writeObject(java.io.ObjectOutputStream outStream)
		throws IOException
	{
		int nullFieldMask = 0x00;
		nullFieldMask |= (this.getName() == null) ? NAME : 0;
		nullFieldMask |= (this.getValue() == null) ? VALUE : 0;
		nullFieldMask |= (this.getComment() == null) ? COMMENT : 0;
		nullFieldMask |= (this.getDomain() == null) ? DOMAIN : 0;
		nullFieldMask |= (this.getExpiryDate() == null) ? EXPIRY_DATE : 0;
		nullFieldMask |= (this.getPath() == null) ? PATH : 0;
        
		outStream.write(nullFieldMask);
		
		if ((nullFieldMask & NAME) == 0)
		{
			outStream.writeUTF(this.getName());
		}
		
		if ((nullFieldMask & VALUE) == 0)
		{
			outStream.writeUTF(this.getValue());
		}
		
		if ((nullFieldMask & COMMENT) == 0)
		{
			outStream.writeUTF(this.getComment());
		}
		
		if ((nullFieldMask & DOMAIN) == 0)
		{
			outStream.writeUTF(this.getDomain());
		}
		
		if ((nullFieldMask & EXPIRY_DATE) == 0)
		{
			outStream.writeObject(this.getExpiryDate());
		}
	
		if ((nullFieldMask & PATH) == 0)
		{
			outStream.writeUTF(this.getPath());
		}
		
		outStream.writeBoolean(this.isSecure());
		outStream.writeInt(this.getVersion());
	}
	
	private void readObject(java.io.ObjectInputStream inStream)
		throws IOException, ClassNotFoundException
	{
		int nullFieldMask = inStream.readInt();
		
		String name = null;
		String value = null;
		if ((nullFieldMask & NAME) == 0)
		{
			name = inStream.readUTF();
		}
		
		if ((nullFieldMask & VALUE) == 0)
		{
			value = inStream.readUTF();
		}
		
		BasicClientCookie cookie = new BasicClientCookie(name, value);
		
		if ((nullFieldMask & COMMENT) == 0)
		{
			cookie.setComment(inStream.readUTF());
		}
		
		if ((nullFieldMask & DOMAIN) == 0)
		{
			cookie.setDomain(inStream.readUTF());
		}
		
		if ((nullFieldMask & EXPIRY_DATE) == 0)
		{
			cookie.setExpiryDate((java.util.Date)(inStream.readObject()));
		}
		
		if ((nullFieldMask & PATH) == 0)
		{
			cookie.setPath(inStream.readUTF());
		}
		
		cookie.setSecure(inStream.readBoolean());
		cookie.setVersion(inStream.readInt());
		
		this._cookie = cookie;
	}
	
	@Override
	public String toString()
	{
		if (this._cookie == null)
		{
			return null;
		}
		else
		{
			return this._cookie.toString();
		}
	}

}
