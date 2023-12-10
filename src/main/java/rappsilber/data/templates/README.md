###Templates

Any .conf file will be presented as a directly loadable config file. If the first line in the file starts with
```
#==
```
anything in that line besides the initial `#==` will be displayed as descriptions along the filename. E.g.

if a file `example.conf` starts with
```
#== this is an example configuration template
```

The GUI will have it in the list of templates as:


```
example -- this is an example configuration template
```

