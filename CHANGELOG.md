# Changelog

All notable changes to this project will be documented in this file.

**Warning:** Features marked as *alpha* may change or be removed in a future release without notice. Use with caution.

<!--## [Unreleased]-->

## [2.1.0]

### Added

* (*alpha*) A new Publication `SearchService` to search through the resources' content, with a default implementation `StringSearchService`.
* `ContentProtection.Scheme` can be used to identify protection technologies using unique URI identifiers.
* `Link` objects from archive-based publication assets (e.g. an EPUB/ZIP) have additional properties for entry metadata.
    ```json
    "properties" {
        "archive": {
            "entryLength": 8273,
            "isEntryCompressed": true
        }
    }
    ```

### Changed

* Upgraded to Kotlin 1.5.31 and Gradle 7.1.1

### Fixed

* Crash with `HttpRequest.setPostForm()` on Android 6.
* HREF normalization when a resource path contains special characters.


## [2.0.0]

### Added

* `HttpFetcher` is a new publication fetcher able to serve remote resources through HTTP.
    * The actual HTTP requests are performed with an instance of `HttpClient`.
* `HttpClient` is a new protocol exposing a high level API to perform HTTP requests.
    * `DefaultHttpClient` is an implementation of `HttpClient` using standard `HttpURLConnection` APIs. You can use `DefaultHttpClient.Callback` to customize how requests are created and even recover from errors, e.g. to implement Authentication for OPDS.
    * You can provide your own implementation of `HttpClient` to Readium APIs if you prefer to use a third-party networking library.


## [2.0.0-beta.2]

### Added

* `Publication.Service.Context` now holds a reference to the parent `Publication`. This can be used to access other services from a given `Publication.Service` implementation.
* The default `LocatorService` implementation can be used to get a `Locator` from a global progression in the publication.
  * `publication.locateProgression(0.5)`

### Fixed

* [#129](https://github.com/readium/r2-shared-kotlin/issues/129) Improve performances when reading deflated ZIP resources.
  * For example, it helps with large image-based FXL EPUB which used to be slow to render.
* [#136](https://github.com/readium/r2-shared-kotlin/issues/136) `null` values in JSON string properties are now properly parsed as nullable types, instead of the string `"null"`


## [2.0.0-beta.1]

### Added

* `PublicationAsset` is a new interface which can be used to open a publication from various medium, such as a file, a remote URL or a custom source.
  * `File` was replaced by `FileAsset`, which implements `PublicationAsset`.

### Changed

* Upgraded to Kotlin 1.4.10.
* `Format` got merged into `MediaType`, to simplify the media type APIs.
  * You can use `MediaType.of()` to sniff the type of a file or bytes.
      * All the `MediaType.of()` functions are now suspending to prevent deadlocks with `runBlocking`.
  * `MediaType` has now optional `name` and `fileExtension` properties.
  * Some publication formats can be represented by several media type aliases. Using `mediaType.canonicalMediaType()` will give you the canonical media type to use, for example when persisting the file type in a database. All Readium APIs are already returning canonical media types, so it only matters if you create a `MediaType` yourself from its string representation.
* `ContentLayout` is deprecated, use `publication.metadata.effectiveReadingProgression` to determine the reading progression of a publication instead.


## [2.0.0-alpha.2]

### Added

* The [Publication Services API](https://readium.org/architecture/proposals/004-publication-helpers-services) allows to extend a `Publication` with custom implementations of known services. This version ships with a few predefined services:
  * `PositionsService` provides a list of discrete locations in the publication, no matter what the original format is.
  * `CoverService` provides an easy access to a bitmap version of the publication cover.
* The [Composite Fetcher API](https://readium.org/architecture/proposals/002-composite-fetcher-api) can be used to extend the way publication resources are accessed.
* Support for exploded directories for any archive-based publication format.
* [Content Protection](https://readium.org/architecture/proposals/006-content-protection) handles DRM and other format-specific protections in a more systematic way.
  * LCP now ships an `LcpContentProtection` implementation to be plugged into the `Streamer`.
  * You can add custom `ContentProtection` implementations to support other DRMs by providing an instance to the `Streamer`.

### Changed

* [The `Publication` and `Container` types were merged together](https://readium.org/architecture/proposals/003-publication-encapsulation) to offer a single interface to a publication's resources.
  * Use `publication.get()` to read the content of a resource, such as the cover. It will automatically be decrypted if a `ContentProtection` was attached to the `Publication`.

### Fixed

* `OutOfMemoryError` occuring while opening large publications are now caught to prevent crashes. They are reported as `Resource.Exception.OutOfMemory`.
* Readium can now open PDF documents of any size without crashing. However, LCP protected PDFs are still limited by the available memory.


## [2.0.0-alpha.1]

### Added

* Support for [Positions List](https://github.com/readium/architecture/tree/master/models/locators/positions), which provides a list of discrete locations in a publication and can be used to implement an approximation of page numbers.
  * Get the visible position from the current `Locator` with `locations.position`.
  * The total number of positions can be retrieved with `publication.positions().size`. It is a suspending function because computing positions the first time can be expensive. 
* The new [Format API](https://github.com/readium/architecture/blob/master/proposals/001-format-api.md) simplifies the detection of file formats, including known publication formats such as EPUB and PDF.
  * [A format can be "sniffed"](https://github.com/readium/architecture/blob/master/proposals/001-format-api.md#sniffing-the-format-of-raw-bytes) from files, raw bytes or even HTTP responses.
  * Reading apps are welcome to [extend the API with custom formats](https://github.com/readium/architecture/blob/master/proposals/001-format-api.md#supporting-a-custom-format).
  * Using `Link.mediaType.matches()` is now recommended [to safely check the type of a resource](https://github.com/readium/architecture/blob/master/proposals/001-format-api.md#mediatype-class).
  * [More details about the Kotlin implementation can be found in the pull request.](https://github.com/readium/r2-shared-kotlin/pull/100)
* In `Publication` shared models:
  * Support for the [Presentation Hints](https://readium.org/webpub-manifest/extensions/presentation.html) extension.
  * Support for OPDS holds, copies and availability in `Link`, for library-specific features.
  * Readium Web Publication Manifest extensibility is now supported for `Publication`, `Metadata`, link's `Properties` and locator's `Locations`, which means that you are now able to access custom JSON properties in a manifest [by creating Kotlin extensions on the shared models](https://github.com/readium/r2-shared-kotlin/blob/a4e5b4461d6ce9f989a79c8f912f3cbdaff4667e/r2-shared/src/main/java/org/readium/r2/shared/publication/opds/Properties.kt#L16).

### Changed

* [The `Publication` shared models underwent an important refactoring](https://github.com/readium/r2-shared-kotlin/pull/88) and some of these changes are breaking. [Please refer to the migration guide to update your codebase](https://github.com/readium/r2-testapp-kotlin/blob/develop/MIGRATION-GUIDE.md).
  * All the models are now immutable data classes, to improve code safety. This should not impact reading apps unless you created `Publication` or other models yourself.
  * A few types and enums were renamed to follow the Google Android Style coding convention better. Just follow deprecation warnings to update your codebase.

### Deprecated

* `R2SyntheticPageList` was replaced by the aforementioned Positions List and can be safely removed from your codebase.

### Fixed

* **Important:** [Publications parsed from large manifests could crash the application](https://github.com/readium/r2-testapp-kotlin/issues/286) when starting a reading activity. To fix this, **`Publication` must not be put in an `Intent` extra anymore**. Instead, [use the new `Intent` extensions provided by Readium](https://github.com/readium/r2-testapp-kotlin/pull/303). This solution is a crutch until [we move away from `Activity` in the Navigator](https://github.com/readium/r2-navigator-kotlin/issues/115) and let reading apps handle the lifecycle of `Publication` themselves.
* [The local HTTP server was broken](https://github.com/readium/r2-testapp-kotlin/pull/306) when provided with publication filenames containing invalid characters.
* XML namespace prefixes are now properly supported when an author chooses unusual ones (contributed by [@qnga](https://github.com/readium/r2-shared-kotlin/pull/85)).
* The `AndroidManifest.xml` is not forcing anymore `allowBackup` and `supportsRtl`, to let reading apps manage these features themselves (contributed by [@twaddington](https://github.com/readium/r2-shared-kotlin/pull/93)).


[unreleased]: https://github.com/readium/r2-shared-kotlin/compare/master...HEAD
[2.0.0-alpha.1]: https://github.com/readium/r2-shared-kotlin/compare/1.1.6...2.0.0-alpha.1
[2.0.0-alpha.2]: https://github.com/readium/r2-shared-kotlin/compare/2.0.0-alpha.1...2.0.0-alpha.2
[2.0.0-beta.1]: https://github.com/readium/r2-shared-kotlin/compare/2.0.0-alpha.2...2.0.0-beta.1
[2.0.0-beta.2]: https://github.com/readium/r2-shared-kotlin/compare/2.0.0-beta.1...2.0.0-beta.2
[2.0.0]: https://github.com/readium/r2-shared-kotlin/compare/2.0.0-beta.2...2.0.0
[2.1.0]: https://github.com/readium/r2-shared-kotlin/compare/2.0.0...2.1.0

