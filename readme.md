# metabook

Adam\'s collection of notes, useful dev namespaces, and important
lessons while working at and with Metabase 😁

This project has a lose and evolving structure, but the basics are:

-   this org file contains the deps and clerk server notes and code to
    get things going.
-   other helpful functions and namespaces may be defined here as time
    goes on.
-   there is a list of notebooks here which link to the `./notebooks`
    folder. This is where specific project notebooks should go.
-   all useful sources are tangled into `./src/metabook`, these can then
    be loaded by other notebooks.

This might also be useful as a starting point for other Metabase devs to
set up Clerk for themselves.

# setup

## prereqs

Some basics for your dev setup should be handled outside of this
project.

-   have your own gitignore in `~/.config/git/ignore` and include:

    -   .clerk/

-   clone this repository into `metabase/local/src/nocommit/`

-   if you are writing in .org files, consider using an elisp function
    to auto-generate .md files from .org:

    ``` elisp
    (defun export-md-on-save-org-mode-file ()
      (let ((filename
            (buffer-file-name)))
        (when (and (string-match-p
                    (regexp-quote ".org") (message "%s" (current-buffer)))
                   (not (string-match-p
                         (regexp-quote "[") (message "%s" (current-buffer)))))
          (shell-command
           (concat "pandoc -f org -t markdown -o " filename ".md " filename)))))

    (add-hook 'after-save-hook 'export-md-on-save-org-mode-file)
    ```

    -   notice that this uses `pandoc`, so that will need to be
        installed on your system. This is done because Org\'s .md
        exporter doesn\'t play nicely with Clerk at the moment (code
        blocks don\'t end up being recognized as blocks to run/syntax
        highlight).

-   set up your deps according to the next section.

## deps

I want to use clerk inside the entire working app of Metabase. To have
the necessary deps load up with the Metabase source without changing any
of the Metabase dependencies, I\'ve got the following aliases in
\~\~./clojure/deps.edn\~, which gets merged with every Clojure project
😎.

``` clojure
{:aliases
 {:cider/nrepl
  {:extra-deps {nrepl/nrepl       {:mvn/version #_ "RELEASE" "0.9.0"}
                cider/cider-nrepl {:mvn/version #_ "RELEASE" "0.27.4"}}
   :main-opts  ["-m" "nrepl.cmdline"
                "--port" "54321"
                "--middleware" "[cider.nrepl/cider-middleware]"]}

  :reveal-cider
  {:extra-deps {vlaaad/reveal     {:mvn/version "RELEASE"}
                nrepl/nrepl       {:mvn/version "RELEASE"}
                cider/cider-nrepl {:mvn/version "RELEASE"}}
   :main-opts  ["-m" "nrepl.cmdline"
                "--port" "54321"
                "--middleware" "[vlaaad.reveal.nrepl/middleware cider.nrepl/cider-middleware]"]}

  :clerk
  {:extra-deps {io.github.nextjournal/clerk {:mvn/version "RELEASE"}}}}}
```

## running

Base the run off of the `metabuild` bash function (borrowed from Dan
Sutton). I\'ve got the source shown below, and have it copied into my
`~/.zshrc` file.

``` bash
metabuild () {
    cd $MB_DIR
    source ${MB_SCRATCH_DIR}/set-env.sh
    print "DB: $MB_DB_CONNECTION_URI"
    print 'clj -M:dev:ee:ee-dev:drivers:drivers-dev:reveal-cider:clerk'
    print 'Connect to nrepl server at localhost:54321'
    clj -M:dev:ee:ee-dev:drivers:drivers-dev:reveal-cider:clerk
}
```

Then, in a separate terminal, so that if emacs crashes, we don\'t lose
our running REPL, you can do:

``` shell
source ~/.zshrc
metabuild

```

# Usage

To use Clerk, you first have to start the server, which will then watch
the files in `metabase/local/src/nocommit/metabook/notebooks`. To run
the Clerk server, load `metabook.server` and run:

``` clojure
(load-file "local/src/nocommit/metabook/src/metabook/server.clj")
(metabook.server/server-start!)
```

Which will start a Clerk Server on Port 7891. You can edit the server
code and change it up as you wish, of course.

Now, any time you make a change to a `.md` or `.clj` file in the
notebooks folder, Clerk will eval and render that file!

# notebooks

Some of my notebooks.

-   [metabasics](./notebooks/metabasics.org)
-   [sso](./notebooks/sso.org)
-   [query-processor](./notebooks/query-processor.org)
