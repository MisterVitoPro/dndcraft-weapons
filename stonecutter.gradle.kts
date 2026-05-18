plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.4"

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    group = "project"
    ofTask("build")
}

stonecutter registerChiseled tasks.register("chiseledRunGametest", stonecutter.chiseled) {
    group = "project"
    ofTask("runGametest")
}

stonecutter registerChiseled tasks.register("chiseledTest", stonecutter.chiseled) {
    group = "project"
    ofTask("test")
}

// Phase 6: chiseled wrapper for generateWiki so it can run via the per-version
// Stonecutter pipeline (shared sources contain //? directives that require
// Stonecutter source replacement to be active during compileKotlin).
stonecutter registerChiseled tasks.register("chiseledGenerateWiki", stonecutter.chiseled) {
    group = "wiki"
    ofTask("generateWiki")
}

