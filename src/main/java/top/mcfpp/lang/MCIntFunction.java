package top.mcfpp.lang;

import org.jetbrains.annotations.NotNull;
import top.mcfpp.Project;
import top.mcfpp.annotations.MCFPPNative;
import top.mcfpp.lib.Function;

public class MCIntFunction {

    @MCFPPNative
    public static void test(@NotNull Var[] vars, CanSelectMember caller) {
        //�����в���
        Function.Companion.addCommand("say " + caller);
    }

    @MCFPPNative
    public static void qwq(){

    }
}
