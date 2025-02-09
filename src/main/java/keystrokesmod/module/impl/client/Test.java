package keystrokesmod.module.impl.client;

import keystrokesmod.module.Module;

public class Test extends Module {

    public Test() {
        super("Test", category.client);
    }

    @Override
    public void onEnable() {
        System.out.println("Hello, World!");
        disable();
    }
}