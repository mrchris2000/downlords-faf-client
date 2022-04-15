package com.faforever.client.remote.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class QDataWriter extends Writer {

  public static final Charset CHARSET = StandardCharsets.UTF_16BE;
  private final OutputStream out;

  public QDataWriter(OutputStream out) {
    this.out = out;
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    out.write(new String(cbuf).substring(off, off + len).getBytes(CHARSET));
  }

  @Override
  public Writer append(CharSequence csq) throws IOException {
    if (csq == null) {
      writeInt32(-1);
      return this;
    }

    byte[] bytes = csq.toString().getBytes(CHARSET);
    return appendWithSize(bytes);
  }

  @Override
  public void flush() throws IOException {
    out.flush();
  }

  @Override
  public void close() throws IOException {
    out.close();
  }

  public void writeInt32(int v) throws IOException {
    out.write((v >>> 24) & 0xFF);
    out.write((v >>> 16) & 0xFF);
    out.write((v >>> 8) & 0xFF);
    out.write(v & 0xFF);
  }

  /**
   * Appends the size of the given byte array to the stream followed by the byte array itself.
   */
  public QDataWriter appendWithSize(byte[] bytes) throws IOException {
    writeInt32(bytes.length);
    out.write(bytes);
    return this;
  }
}
