package to.joe.ftp.ftp;

import java.io.IOException;
import java.io.Reader;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.bouncycastle.jsse.BCExtendedSSLSession;
import org.bouncycastle.jsse.BCSSLSocket;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

public class FTPSClientSSLSessionReuse extends FTPSClient {
	
	private static SSLContext createSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext context = SSLContext.getInstance("TLS", new BouncyCastleJsseProvider());
		TrustManager trustManager = TrustManagerUtils.getValidateServerCertificateTrustManager();
		
		context.init(null, new TrustManager[] {trustManager}, new SecureRandom());
		return context;
	}
	
	private Optional<BCExtendedSSLSession> sessionToResume = Optional.empty();
	
	public FTPSClientSSLSessionReuse() throws KeyManagementException, NoSuchAlgorithmException {
		super(false, createSSLContext());
		
		setEnabledProtocols(new String[] { "TLSv1.2" });
	}
	
	@Override
	protected void _connectAction_(Reader socketIsReader) throws IOException {
		super._connectAction_(socketIsReader);
		
		if (_socket_ instanceof BCSSLSocket) {
			BCSSLSocket sslSocket = (BCSSLSocket) _socket_;
			sessionToResume = Optional.ofNullable(sslSocket.getBCSession());
		}
	}
	
	@Override
	protected void _prepareDataSocket_(Socket socket) throws IOException {
		if (socket instanceof BCSSLSocket) {
			BCSSLSocket sslSocket = (BCSSLSocket) socket;
			sessionToResume.ifPresent(sslSocket::setBCSessionToResume);
		}
	}

}


/*
 * 
 * import org.apache.commons.net.ftp.FTPSClient
import org.apache.commons.net.util.TrustManagerUtils
import org.bouncycastle.jsse.BCExtendedSSLSession
import org.bouncycastle.jsse.BCSSLSocket
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider

object FTPSClientSSLSessionReuse {

  // Create an SSLContext with BouncyCastle
  def createSSLContext(): SSLContext = {
    val context = SSLContext.getInstance("TLS", new BouncyCastleJsseProvider());
    val trustManager = TrustManagerUtils.getValidateServerCertificateTrustManager()

    context.init(null, Array(trustManager), new SecureRandom()) // scalafix:ok
    context
  }
}

class FTPSClientSSLSessionReuse extends FTPSClient(false, FTPSClientSSLSessionReuse.createSSLContext()) {
  // Not thread safe but the base implementation is not thread safe either
  var sessionToResume: Option[BCExtendedSSLSession] = None // scalafix:ok

  // Disable TLSv1.3 as it's not working for session reuse
  setEnabledProtocols(Array("TLSv1.2"))

  override protected def _connectAction_(): Unit = {
    super._connectAction_()
    // Store SSL Session
    if (_socket_.isInstanceOf[BCSSLSocket]) {
      sessionToResume = Some(_socket_.asInstanceOf[BCSSLSocket].getBCSession())
    }
  }

  override protected def _prepareDataSocket_(socket: Socket): Unit = {
    // Reuse SSL Session for data connection
    if (socket.isInstanceOf[BCSSLSocket]) {
      sessionToResume.foreach(session =>
        socket.asInstanceOf[BCSSLSocket].setBCSessionToResume(session)
      )
    }
  }
}
 */