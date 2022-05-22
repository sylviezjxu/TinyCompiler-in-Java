package IR;

public class Register {

    private static int currentRg = 6;
    private static int currentArgRg = 0;

    /** the first 6 registers are allocated for function arguments, rest are passed onto the stack */
    public static Integer nextArgRegister() {
        if (currentArgRg < 6) {
            return currentArgRg++;
        }
        else {
            return null;
        }
    }

    /** returns next available non-arg register */
    public static int nextRegister() {
        return currentRg++;
    }
}
