import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private static Parser ourInstance = new Parser();

    public static Parser getInstance() {
        return ourInstance;
    }

    private Parser() {
    }

    public InetSocketAddress parsePassiveSocket(String message) {
        Pattern pattern = Pattern.compile("\\([0-9]+,[0-9]+,[0-9]+,[0-9]+,[0-9]+,[0-9]+\\)$");
        Matcher matcher = pattern.matcher(message);

        matcher.find();

        String data = message.substring(matcher.start(), matcher.end());
        pattern = Pattern.compile("[0-9]+");
        matcher = pattern.matcher(data);

        StringBuilder hostBuilder = new StringBuilder();
        int first = 0;
        int second = 0;

        int i = 0;
        String item;
        while (i < 6) {
            matcher.find();
            item = data.substring(matcher.start(), matcher.end());
            if (i < 4) {
                hostBuilder.append(item);
                if (i < 3) {
                    hostBuilder.append(".");
                }
            } else if (i == 4) {
                first = Integer.valueOf(item);
            } else {
                second = Integer.valueOf(item);
            }

            i++;
        }

        return new InetSocketAddress(hostBuilder.toString(), first * 256 + second);
    }

    public boolean isPassiveSocket(String message) {
        Pattern pattern = Pattern.compile("\\([0-9]+,[0-9]+,[0-9]+,[0-9]+,[0-9]+,[0-9]+\\)$");
        Matcher matcher = pattern.matcher(message);

        return matcher.find();
    }

    public CmdOneArg parseCommand(String inputStr) {
        String[] command = inputStr.split(" ");

        String arg = command.length < 2 ? "" : command[1];

        return new CmdOneArg(
                command[0],
                arg
        );
    }
}
