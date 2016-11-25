#!/usr/bin/python2
import vk_auth
import json
import urllib2
from urllib import urlencode
import os
import getpass
import sys
from subprocess import check_output

def call_api(method, params, token):
        params.append(("access_token", token))
        url = "https://api.vk.com/method/%s?%s" % (method, urlencode(params))
        return json.loads(urllib2.urlopen(url).read())["response"]

if (len(sys.argv) != 3):
    print "usage: %s email password" % (sys.argv[0])
    os._exit(0)
email = sys.argv[1]
password = sys.argv[2]
client_id = "5745817"
token, user_id, begUrl = vk_auth.auth(email, password, client_id, "status")
print (user_id + " " + token + "\nbegUrl: " + begUrl)

status = check_output(["uptime", "-p"])

call_api("status.set", [("text", "My server's " + status)], token)
print status
