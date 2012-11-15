#!/usr/bin/env python

import urllib2
import time

if __name__ == '__main__':
    start = time.time()
    f = urllib2.urlopen('https://mobile-print-dev.mit.edu/printatmit/update_results/')
    print f.read()
    print "time: " + str(time.time() - start)
