public class CmdOneArg {
    private final String command;
    private final String argument;

    public CmdOneArg(String command, String argument) {
        this.command = command.toLowerCase();
        this.argument = argument;
    }

    public String getCommand() {
        return command;
    }

    public String getArgument() {
        return argument;
    }
}
