import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerTCP {
    private static ServerSocket ss;
    private static Thread serverThread;
    static BlockingQueue<SocketProcessor> q = new LinkedBlockingQueue<SocketProcessor>();

    public static void main(String[] args) throws IOException{

        run();

    }
   public static void run() throws IOException {
        ss = new ServerSocket(9896);
        serverThread = Thread.currentThread();
//       try {
//           ss = new ServerSocket(port);
//       } catch (IOException e) {
//           e.printStackTrace();
//       }
       while (true) {
            Socket s = getNewConn();
            if (serverThread.isInterrupted()) {
                break;
            } else if (s != null){
                try {

                    final SocketProcessor processor = new SocketProcessor(s);
                    final Thread thread = new Thread(processor);
                    thread.setDaemon(true);
                    thread.start();
                    q.offer(processor);
                }
            catch (IOException ignored) {}
            }
        }
    }
    private static Socket getNewConn() {
        Socket s = null;
        try {
            s = ss.accept();
        } catch (IOException e) {
            shutdownServer();
        }
        return s;
    }
    private static synchronized void shutdownServer() {
        for (SocketProcessor s: q) {
            s.close();
        }
        if (!ss.isClosed()) {
            try {

                ss.close();
            } catch (IOException ignored) {}
        }
    }

    private static class SocketProcessor implements Runnable{
        Socket s;
        BufferedReader br;
        BufferedWriter bw;

        SocketProcessor(Socket socketParam) throws IOException {
            s = socketParam;
            br = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));

            bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8") );
        }
        public void run() {
            while (!s.isClosed()) {
                String line = null;
                try {
                    line = br.readLine();
                } catch (IOException e) {
                    close();
                }
                if (line == null) {
                    close();
                } else if ("shutdown".equals(line)) {
                    serverThread.interrupt();
                    try {
                        new Socket("localhost", 9896);

                    } catch (IOException ignored) {
                    } finally {
                        shutdownServer();
                    }
                } else {
                    String hi = "HIHIHIHIH";
                    for (SocketProcessor sp:q) {

                        sp.send(line);
                    }
                }
            }
        }
        public synchronized void send(String line) {
            try {
                bw.write(line);
                bw.write("\n");
                bw.flush();
            } catch (IOException e) {
                close();

            }

        }
        public synchronized void close() {
            q.remove(this);
            if (!s.isClosed()) {
                try {
                    s.close();
                } catch (IOException ignored) {}
            }
        }
        @Override
        protected void finalize() throws Throwable {

            super.finalize();

            close();

        }
    }
}

