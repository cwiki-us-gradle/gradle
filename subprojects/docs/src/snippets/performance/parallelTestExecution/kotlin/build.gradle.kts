plugins {
    id("java-library")
}

// tag::parallel-4[]
// tag::parallel-calculated[]
// tag::fork-every[]
// tag::disable-reports[]
tasks.withType<Test>().configureEach {
// end::parallel-4[]
// end::parallel-calculated[]
// end::fork-every[]
// end::disable-reports[]

// tag::parallel-4[]
    maxParallelForks = 4
// end::parallel-4[]

// tag::parallel-calculated[]
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
// end::parallel-calculated[]

// tag::fork-every[]
    setForkEvery(100)
// end::fork-every[]

// tag::disable-reports[]
    reports.html.isEnabled = false
    reports.junitXml.isEnabled = false
// end::disable-reports[]

// tag::parallel-4[]
// tag::parallel-calculated[]
// tag::fork-every[]
// tag::disable-reports[]
}
// end::parallel-4[]
// end::parallel-calculated[]
// end::fork-every[]
// end::disable-reports[]


// tag::fork-java[]
tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
}
// end::fork-java[]



