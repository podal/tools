import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.Date;

public class JTunnel implements Runnable {
	private final InputStream in;
	private final OutputStream out;
	private final Socket[] sockets;
	private JTunnel[] tunnels;

	public JTunnel(final Socket from, final Socket to) throws IOException {
		in = from.getInputStream();
		out = to.getOutputStream();
		sockets = new Socket[] { from, to };
	}

	public static void main(String[] args) throws IOException {
		int listenenPort = Integer.parseInt(args[0]);
		int remotePort = Integer.parseInt(args[1]);
		String remoteHost = args[2];
		ServerSocket ss = new ServerSocket(listenenPort);
		while (true) {
			Socket accessSocket = ss.accept();
			Socket remoteSocket = new Socket(remoteHost, remotePort);
			JTunnel tunnel = new JTunnel(accessSocket, remoteSocket);
			JTunnel tunnel2 = new JTunnel(remoteSocket, accessSocket);
			JTunnel[] tunnels = new JTunnel[] { tunnel, tunnel2 };
			tunnel.setClose(tunnels);
			tunnel2.setClose(tunnels);
			System.out.println(MessageFormat.format("{0,date,short} {0,time,short} Open tunnel.",new Object[]{new Date()}));
			fork(tunnels);
		}
	}

	private void setClose(JTunnel[] tunnels) {
		this.tunnels = tunnels;
	}

	private static void fork(Runnable[] runnables) {
		for (int i = 0; i < runnables.length; i++) {
			Thread thread = new Thread(runnables[i]);
			thread.setDaemon(true);
			thread.start();
		}
	}

	public void run() {
		try {
			byte[] bs = new byte[1024];
			int a;
			while ((a = in.read(bs)) != -1) {
				out.write(bs, 0, a);
			}
		} catch (IOException e) {
		} finally {
			closeAll();
		}
	}

	private void closeAll() {
		for (int i = 0; i < tunnels.length; i++) {
			tunnels[i].close();
		}
	}

	private void close() {
		close(in);
		close(out);
		for (int i = 0; i < sockets.length; i++) {
			close(sockets[i]);
		}
	}

	private void close(Closeable close) {
		try {
			close.close();
		} catch (Exception e) {
		}
	}

	private void close(Socket close) {
		try {
			close.close();
		} catch (Exception e) {
		}
	}
}
