import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class FtpClient implements DataReaderActionListener {

    private final List<String> availableCommands = Arrays.asList(
            "user",
            "pass",
            "cwd",
            "pwd",
            "mkd",
            "rmd",
            "dele",
            "pasv",
            "list",
            "nlst",
            "retr",
            "stor",
            "quit",
            "help"
    );

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    private Socket passiveSocket;

    private AtomicBoolean isRunningDataThread = new AtomicBoolean(false);

    private CmdOneArg lastCommand;

    private DataReader dataThread;
    private Thread mainReader;

    public FtpClient(String host, int port) throws IOException {

        socket = new Socket(host, port);

        this.reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );
        this.writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream())
        );
    }

    public void start() throws IOException {
        startMainReader();
        startMainWriter();
    }

    private void startMainReader() {
        mainReader = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {

                    System.out.println(line);

                    if (Parser.getInstance().isPassiveSocket(line)) {
                        InetSocketAddress inetSocketAddress = Parser.getInstance().parsePassiveSocket(line);
                        passiveSocket = new Socket(
                                inetSocketAddress.getHostName(),
                                inetSocketAddress.getPort()
                        );
                    } else {
                        if (dataThread != null && dataThread.isAlive()) {
                            try {
                                dataThread.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        mainReader.start();
    }

    private void startMainWriter() throws IOException {
        Scanner in = new Scanner(System.in);

        String inputStr;
        while (!(inputStr = in.nextLine()).equalsIgnoreCase("quit")) {

            lastCommand = Parser.getInstance().parseCommand(inputStr);

            if (!availableCommands.contains(lastCommand.getCommand())) {
                System.out.println("Недопустимая команда!");
                continue;
            }

            if (isRunningDataThread.get()) {
                System.out.println("Подождите. Выполняется получение данных.");
                continue;
            }

            if (lastCommand.getCommand().equalsIgnoreCase("retr")
                    || lastCommand.getCommand().equalsIgnoreCase("stor")
                    || lastCommand.getCommand().equalsIgnoreCase("list")
                    || lastCommand.getCommand().equalsIgnoreCase("nlst")
            ) {
                if (passiveSocket == null) {
                    System.out.println("Необходимо установить соединение для передачи данных. Используйте команду PASV.");
                    continue;
                }

                if (passiveSocket != null) {
                    dataThread = new DataReader(passiveSocket, this);
                    dataThread.setCurrentCommand(lastCommand);
                    dataThread.start();

                    isRunningDataThread.set(true);

                    passiveSocket = null;
                } else {
                    System.out.println("Не удалось установить пассивное соединение!");
                    passiveSocket = null;
                    continue;
                }

                if (lastCommand.getCommand().equalsIgnoreCase("stor")) {
                    String[] temp = lastCommand.getArgument().split("\\\\");
                    String fileName = temp[temp.length - 1];

                    writer.write("stor " + fileName);
                    writer.newLine();
                    writer.flush();
                } else {
                    writer.write(inputStr);
                    writer.newLine();
                    writer.flush();
                }
            } else if (lastCommand.getCommand().equalsIgnoreCase("help")) {
                System.out.println("Доступные команды:");
                for (String item : availableCommands) {
                    System.out.println(item);
                }
            } else {
                writer.write(inputStr);
                writer.newLine();
                writer.flush();
            }
        }

        writeMsg(inputStr);

        try {
            if (dataThread != null) {

                if (isRunningDataThread.get()) {
                    writeMsg("abor");
                }

                dataThread.join();
            }

            mainReader.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        socket.close();
    }

    @Override
    public void onDataReaderFinish() {
        isRunningDataThread.set(false);
    }

    private void writeMsg(String msg) throws IOException {
        writer.write(msg);
        writer.newLine();
        writer.flush();
    }
}
