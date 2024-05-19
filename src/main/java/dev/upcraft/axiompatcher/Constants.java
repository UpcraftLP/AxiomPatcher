package dev.upcraft.axiompatcher;

import java.net.URI;

public class Constants {

    public static final String USER_AGENT = "UpcraftLP/AxiomPatcher/%s".formatted(Util.getVersion());

    public static final String AXIOM_MODRINTH_ID = "N6n5dqoA";

    public static final URI LIST_AXIOM_VERSIONS_ENDPOINT = URI.create("https://api.modrinth.com/v2/project/%s/version".formatted(AXIOM_MODRINTH_ID));
}
