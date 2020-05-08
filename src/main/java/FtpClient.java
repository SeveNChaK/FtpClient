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

public class FtpClient {

    private final List<String> availableCommands = Arrays.asList(
            "user", "pass", "cwd", "mkd", "rmd", "dele", "pasv", "list", "nlst", "retr", "stor", "quit"
    );

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    private boolean isPasvConnected = false;
    private CmdOneArg lastCommand;
    private DataReader dataThread;

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
        new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {

                    System.out.println(line);

                    if (Parser.getInstance().isPassiveSocket(line)) {
                        isPasvConnected = true;

                        InetSocketAddress inetSocketAddress = Parser.getInstance().parsePassiveSocket(line);
                        Socket pasvSocket = new Socket(
                                inetSocketAddress.getHostName(),
                                inetSocketAddress.getPort()
                        );
                        dataThread = new DataReader(pasvSocket);
                    }
                }
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }).start();
    }

    private void startMainWriter() throws IOException {
        Scanner in = new Scanner(System.in);

        String inputStr;
        while (!(inputStr = in.nextLine()).equalsIgnoreCase("quit")) {

            lastCommand = Parser.getInstance().parseCommand(inputStr);

            //TODO проверка не выполняется ли че

            if (!availableCommands.contains(lastCommand.getCommand())) {
                System.out.println("Не допустимая команда!");
                continue;
            }

            if (lastCommand.getCommand().equalsIgnoreCase("retr")
                    || lastCommand.getCommand().equalsIgnoreCase("stor")
                    || lastCommand.getCommand().equalsIgnoreCase("list")
                    || lastCommand.getCommand().equalsIgnoreCase("nlst")
            ) {
                if (!isPasvConnected) {
                    System.out.println("Необходимо установить соединение для передачи данных. Используйте команду PASV.");
                    continue;
                }

                dataThread.setCurrentCommand(lastCommand);
                dataThread.start();

                isPasvConnected = false;

                //TODO
                if (lastCommand.getCommand().equalsIgnoreCase("stor")) {
                    String[] temp = lastCommand.getArgument().split("\\\\");
                    String fileName = temp[temp.length - 1];

                    writer.write("stor " + fileName);
                    writer.newLine();
                    writer.flush();
                } else  {
                    writer.write(inputStr);
                    writer.newLine();
                    writer.flush();
                }
            } else {
                writer.write(inputStr);
                writer.newLine();
                writer.flush();
            }
        }

        if (dataThread != null) {
            dataThread.close();
        }
        socket.close();
    }
}
