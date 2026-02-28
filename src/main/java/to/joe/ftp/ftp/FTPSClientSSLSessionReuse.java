package to.joe.ftp.ftp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Locale;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;

import org.apache.commons.net.ftp.FTPSClient;

/**
 * Modified version of {@link FTPSClient} to allow for session reuse, as required by most FTPd these days. See "FTPS Nonsense" section in README for more details.
 */
public class FTPSClientSSLSessionReuse extends FTPSClient {
	
	@Override
	protected void _prepareDataSocket_(final Socket socket) throws IOException {
	    if (socket instanceof SSLSocket) {
	        // Control socket is SSL
	        final SSLSession session = ((SSLSocket) _socket_).getSession();
	        if (session.isValid()) {
	            final SSLSessionContext context = session.getSessionContext();
	            try {
	                final Field sessionHostPortCache = context.getClass().getDeclaredField("sessionHostPortCache");
	                sessionHostPortCache.setAccessible(true);
	                final Object cache = sessionHostPortCache.get(context);
	                final Method method = cache.getClass().getDeclaredMethod("put", Object.class, Object.class);
	                method.setAccessible(true);
	                method.invoke(cache, String
	                        .format("%s:%s", socket.getInetAddress().getHostName(), String.valueOf(socket.getPort()))
	                        .toLowerCase(Locale.ROOT), session);
	                method.invoke(cache, String
	                        .format("%s:%s", socket.getInetAddress().getHostAddress(), String.valueOf(socket.getPort()))
	                        .toLowerCase(Locale.ROOT), session);
	            } catch (NoSuchFieldException e) {
	                throw new IOException(e);
	            } catch (Exception e) {
	                throw new IOException(e);
	            }
	        } else {
	            throw new IOException("Invalid SSL Session");
	        }
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
