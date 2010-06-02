import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public final class JTale extends Reader {
	private final FileChannel fc;

	private long size;

	private final int start;

	private final boolean follow;

	public JTale(String string, int start, boolean follow) throws IOException {
		FileInputStream fis = new FileInputStream(string);
		fc = fis.getChannel();
		size = getStartForRow(fc, start);
		this.start = start;
		this.follow = follow;
	}

	private static long getStartForRow(FileChannel fc, int startRow) throws IOException {
		long size = fc.size();
		int w = (int) Math.min(1024, fc.size());
		ByteBuffer bb = ByteBuffer.allocate(w);
		int count = 0;
		do {
			long newSize = size - w;
			fc.read(bb);
			byte[] bs = bb.array();
			System.out.println("newSize="+newSize);
			int i = bs.length;
			while ((i = lastIndexOf(bs, new byte[] { 10, 13 }, i - 1)) != -1) {
				count++;
				if (count >= startRow) {
					return newSize + i + 1;
				}
				System.out.println(i);
			}
			size = newSize;
		} while (size != 0);
		return 0;
	}

	private static int lastIndexOf(byte[] bs, byte[] find, int start) {
		for (int i = start; i >= 0; i--) {
			for (int j = 0; j < find.length; j++) {
				if (bs[i] == find[j]) {
					return i;
				}
			}
		}
		return -1;
	}

	public static void main(String[] args) throws IOException {
		String grep = null;
		int start = 5;
		boolean help = false;
		boolean follow = false;
		for (int i = 0; i < args.length - 1; i++) {
			if (args[i].startsWith("-g")) {
				grep = args[i + 1];
				i++;
			} else if (args[i].startsWith("-h")) {
				help = true;
			} else if (args[i].startsWith("-f")) {
				follow = true;
			} else if (args[i].startsWith("-")) {
				start = Integer.parseInt(args[i].substring(1));
			}
		}
		if (help || args.length == 0 || args[args.length - 1].startsWith("-")) {
			printUsage();
			return;
		}
		JTale lft = new JTale(args[args.length - 1], start, follow);
		BufferedReader br = new BufferedReader(lft);
		String line;
		while ((line = br.readLine()) != null) {
			if (grep == null || line.matches(grep)) {
				System.out.println(line);
			}
		}
	}

	private static void printUsage() {
		System.out.println("usage: java JTail [-h] [-f] [-g <regexp>][-<number>] [file]");
		System.out.println("    This command show the tail of enterd file.");
		System.out.println("");
		System.out.println("DESCRIPTION");
		System.out.println("");
		System.out.println("    The options are as follows:");
		System.out.println("");
		System.out.println("    -h print this help text.");
		System.out.println("");
		System.out.println("    -g <regexp>");
		System.out.println("        Show only the lines that match regexp.");
		System.out.println("");
		System.out.println("    -f follow file, every new line are printed. JTail will not end.");
		System.out.println("        This option follows wnterd file, when the file is extended the new data is");
		System.out.println("        shown. For example if you have a log file that has output and to follow");
		System.out.println("        the file you enter JTail yourLogFile.log and it will print new lines");
		System.out.println("        from that file.");
		System.out.println("");
		System.out.println("    -<number>");
		System.out.println("        Number of lines that will be display before folowing the file");
		System.out.println("");
	}

	public void close() throws IOException {
		fc.close();
	}

	public int read(char[] cbuf, int off, int len) throws IOException {
		long size2 = fc.size();
		if (size2 < size) {
			System.out.println("File is getting smaller, restart follow");
			size = getStartForRow(fc, start);
			size2 = fc.size();
		}
		if (size2 > size) {
			int read = (int) Math.min((long) (len - off), size2 - size);
			ByteBuffer bb = ByteBuffer.allocate(read);
			int count = fc.read(bb, size);
			byte[] bs = bb.array();
			for (int i = 0; i < count; i++) {
				cbuf[i] = (char) (bs[i] & 0xff);
			}
			size += read;
			return count;
		} else {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		return follow ? 0 : -1;
	}
}
