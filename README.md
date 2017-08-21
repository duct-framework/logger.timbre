# Duct logger.timbre

[![Build Status](https://travis-ci.org/duct-framework/logger.timbre.svg?branch=master)](https://travis-ci.org/duct-framework/logger.timbre)

Integrant multimethods for setting up a [Timbre][] logger for the
[Duct][] framework.

[timbre]: https://github.com/ptaoussanis/timbre
[duct]: https://github.com/duct-framework/duct

## Installation

To install, add the following to your project `:dependencies`:

    [duct/logger.timbre "0.3.0"]

## Basic usage

The Timbre configuration is stored in the `:duct.logger/timbre`
key. See the [configuration][] section of the Timbre README for full
explanation.

Three additional keys are defined that reference core Timbre logging
appenders:

* `:duct.logger.timbre/brief`
* `:duct.logger.timbre/println`
* `:duct.logger.timbre/spit`

The brief appender prints to STDOUT only the log message without
timestamp or any other information. It's most useful during
development.

The println and spit appenders are appenders that come with Timbre.
See the [built-in appenders][] section of the Timbre README.

All the appenders also accept a `:min-level` option, so you can one
appender that logs everything, and another appender that logs more
selectively.

A basic configuration that logs messages of "INFO" and above to STDOUT
looks like this:

```clojure
{:duct.logger/timbre
 {:level    :info
  :appender #ig/ref :duct.logger.timbre/println}

 :duct.logger.timbre/println {}}
```

When this configuration is initiated with `integrant.core/init`, the
`:duct.logger/timbre` key is replaced with an implementation of the
`duct.logger/Logger` protocol. See the [duct.logger][] library for how
to make use of this.

[configuration]: https://github.com/ptaoussanis/timbre/blob/master/README.md#configuration
[built-in appenders]: https://github.com/ptaoussanis/timbre/blob/master/README.md#built-in-appenders
[duct.logger]: https://github.com/duct-framework/logger

## Legacy integration

It's often useful to integrate with existing logging solutions. In
this case, we can set the `:set-root-config?` option to true:

```clojure
{:duct.logger/timbre
 {:set-root-config? true
  :level            :info
  :appender         #ig/ref :duct.logger.timbre/println}

 :duct.logger.timbre/println {}}
```

This will call the global `taoensso.timbre/set-config!` function on
`init` with the configuration in the key, and restore the previous
global configuration on `halt!`.

Note that as this changes a global var, only one system can be running
with this option at any one time. Unless you need to interface with
legacy global logging systems, it's recommended you keep this option
off and use the `duct.logger/log` function for logging.

## License

Copyright © 2017 James Reeves

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
