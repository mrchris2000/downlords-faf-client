package com.faforever.client.remote;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.remote.domain.FafServerMessage;
import com.faforever.client.remote.io.QDataInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.Socket;

/**
 * Super class for all server accessors.
 */
public abstract class AbstractServerAccessor implements DisposableBean {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private boolean stopped;
  private QDataInputStream dataInput;

  /**
   * Reads data received from the server and dispatches it. So far, there are two types of data sent by the server: <ol>
   * <li><strong>Server messages</strong> are simple words like ACK or PING, followed by some bytes..</li>
   * <li><strong>Objects</strong> are JSON-encoded objects like preferences or player information. Those are converted into a
   * {@link FafServerMessage}</li> </ol> I'm not yet happy with those terms, so any suggestions are welcome.
   */
  protected void blockingReadServer(Socket socket) throws IOException {
    JavaFxUtil.assertBackgroundThread();

    dataInput = new QDataInputStream(new DataInputStream(new BufferedInputStream(socket.getInputStream())));
    while (!stopped && !socket.isInputShutdown()) {
      dataInput.skipBlockSize();
      String message = dataInput.readQString();

      logger.debug("Message from server: {}", message);

      try {
        onServerMessage(message);
      } catch (Exception e) {
        logger.warn("Error while handling server message: " + message, e);
      }
    }

    logger.info("Connection to server {} has been closed", socket.getRemoteSocketAddress());
  }

  protected abstract void onServerMessage(String message) throws IOException;

  @Override
  public void destroy() throws IOException {
    stopped = true;
    IOUtils.closeQuietly(dataInput);
  }

}
