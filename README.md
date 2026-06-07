# pkg-fit

![Java](https://img.shields.io/badge/Java-17-blue)
![License](https://img.shields.io/badge/License-MIT-green)
[![CI](https://github.com/yashthakare93/pkg-fit/actions/workflows/ci.yml/badge.svg)](https://github.com/yashthakare93/pkg-fit/actions/workflows/ci.yml)
[![Spring Shell](https://img.shields.io/badge/Spring%20Shell-3.2.5-brightgreen)](https://spring.io/projects/spring-shell)

A Java CLI tool that queries the npm registry, resolves package versions with semver ranges, and manages project dependencies — without Node.js overhead.

---

## Requirements

- **Java 17+** — required
- **Maven** — to build
- **Node.js** — optional, for Node version detection

---

## Quick Start

```bash
git clone https://github.com/yashthakare93/pkg-fit.git
cd pkg-fit
mvn package -DskipTests
java -jar target/pkg-fit-0.1.0.jar
```

---

## Commands

| Command | Alias | Description | Flags |
|---------|-------|-------------|-------|
| `init` | — | Create a minimal `package.json` | `[name]` |
| `add` | `a` | Add a dependency | `--dev` `--exact` |
| `install` | `i` | Install packages, auto-selects versions compatible with existing peer deps | `--dev` |
| `remove` | `rm` | Remove one or more dependencies | `--dev` |
| `update` | `up` | Update to latest matching version | `--dev` |
| `list` | `ls` | List installed dependencies | `--dev` |
| `outdated` | `outd` | Check for outdated dependencies | `--dev` |
| `describe` | `desc` | Registry info + installed range + resolved version | |
| `info` | — | Package metadata from npm registry | |
| `search` | — | Search packages on npm | |
| `resolve` | — | Resolve a version from a semver range | |
| `why` | — | Resolution path: skipped versions, peer deps, installed range | |
| `dedupe` | `dd` | Find conflicting versions across deps and devDeps | |
| `purge` | `prune` | Remove all dependencies | `--dev` |

---

## Example Session

```bash
# create project
init my-project

# add deps
add react
add mocha --dev
add lodash@^4.0.0

# install with peer dep compatibility check
i tailwindcss

# check what's stale
outdated

# inspect resolution path
why lodash

# clean up
rm lodash mocha
purge
```

---

## Architecture

```
                    +-----------+
                    |   Shell   |
                    | (14 cmds) |
                    +-----+-----+
                          |
          +---------------+---------------+
          |               |               |
    +-----v------+  +----v-------+  +----v--------+
    |  Registry  |  |  Resolver  |  |  Context    |
    |  Service   |  |  Service   |  |  Service    |
    +-----+------+  +-----+------+  +-----+-------+
          |               |                |
    +-----v------+        |         +------v-------+
    | npm Registry|       |         | package.json |
    +------------+        |         +--------------+
                    +-----v-------+
                    | Compatibility|
                    |  Service     |
                    +--------------+
```

| Service | Responsibility |
|---------|---------------|
| `RegistryService` | HTTP client for npm — fetches metadata, versions, dist-tags |
| `ResolverService` | Semver resolution with skipped-version tracking |
| `ContextService` | Manages `package.json` and Node version detection |
| `CompatibilityService` | Peer dependency compatibility checking during installs |
| `AddService` | Writes dependencies to `package.json` |
| `RemoveService` | Removes dependencies from `package.json` |

---

## Tech Stack

| Component | Version |
|-----------|---------|
| Spring Boot | 3.2.5 |
| Spring Shell | 3.2.5 |
| Java | 17 |
| java-semver | 0.10.2 |
| Jackson | Spring Boot managed |
| Maven | any recent |

---

## Build

```bash
mvn package              # with tests
mvn package -DskipTests  # skip tests
```

Tests: **114**

---

## Project Structure

```
src/main/java/com/pkgfit/
├── commands/         Shell command handlers (14 commands)
├── service/
│   ├── RegistryService       HTTP client for npm registry
│   ├── ResolverService       Semver resolution with skipped-version tracking
│   ├── ContextService        Project context (package.json, Node version)
│   ├── CompatibilityService  Peer dependency compatibility checking
│   ├── AddService            Write dependencies to package.json
│   └── RemoveService         Remove dependencies from package.json
├── model/            Data records (ProjectContext, ResolutionResult)
└── util/             Utilities (PackageName parser)

src/test/java/com/pkgfit/
├── commands/         Command unit tests (mocked services)
└── service/          Service unit tests
```

---

## License

MIT