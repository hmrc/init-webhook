
# init-webhook

[![Build Status](https://travis-ci.org/hmrc/init-webhook.svg?branch=master)](https://travis-ci.org/hmrc/init-webhook) [ ![Download](https://api.bintray.com/packages/hmrc/releases/init-webhook/images/download.svg) ](https://bintray.com/hmrc/releases/init-webhook/_latestVersion)

This is a standalone tool to create new webhooks in GitHub.

```bash
Usage: init-webhook [options]

  --help                   prints this usage text
  --github-token <value>   github token
  --github-org <value>     the name of the github organization. Defaults to hmrc
  --repositories <repo1>,<repo2>...
                           the name of the github repository
  --content-type 'application/json', 'application/x-www-form-urlencoded'
                           the body format sent to the Webhook
  --webhook-url <value>    the url to add as a github Webhook
  --webhook-secret <value>
                           an optional webhook secret key to be added to the Webhook
  --events <event1>,<event2>...
                           optional comma separated events for notification. Defaults to: pull_request_review_comment, release, issues, pull_request, status
  --verbose                verbose mode (debug logging). Defaults to false
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
    
    
    