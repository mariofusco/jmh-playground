package org.jmhplayground.infra;

import java.util.UUID;

import org.openjdk.jmh.annotations.CompilerControl;

public class Main {

    // -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining -XX:-BackgroundCompilation -XX:-TieredCompilation -XX:+LogCompilation
    // https://wiki.openjdk.org/display/HotSpot/Server+Compiler+Inlining+Messages


    public static void main(String[] args) {
        int count = 11_000;
        boolean useSwitch = false;

        long equals = 0;
        String uuid = UUID.randomUUID().toString();

        for (int i = 0; i < count; i++) {
            equals = getEquals(useSwitch, i, uuid, equals);
        }

        System.out.println(equals);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    private static long getEquals(boolean useSwitch, int i, String uuid, long equals) {
        if (useSwitch) {
            equals = getEquals6(i, uuid, equals);
        } else {
            if (!uuid.equals(""+ i)) {
                equals++;
            }
        }
        return equals;
    }

    private static long getEquals6(int i, String uuid, long equals) {
        switch(i %6) {
            case 0:
                if (!uuid.equals(""+ i)) {
                    equals++;
                }
                break;
            case 1:
                if (!uuid.equals(""+ i)) {
                    equals++;
                }
                break;
            case 2:
                if (!uuid.equals(""+ i)) {
                    equals++;
                }
                break;
            case 3:
                if (!uuid.equals(""+ i)) {
                    equals++;
                }
                break;
            case 4:
                if (!uuid.equals(""+ i)) {
                    equals++;
                }
                break;
            case 5:
                if (!uuid.equals(""+ i)) {
                    equals++;
                }
                break;
        }
        return equals;
    }
}
