package com.kumuluz.ee.health.checks;

public abstract class KumuluzHealthCheck {

    protected String kumuluzBaseHealthConfigPath = "kumuluzee.health.checks.";

    public abstract String name();

    public abstract boolean initSuccess();

}
