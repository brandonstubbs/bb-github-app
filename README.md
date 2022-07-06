# Babashka Github Application

This is an example [babashka](https://github.com/babashka) script that can
authenticate as a Github application. To demo this, the application interacts
with the Github Checks API. As the main focus of this is for the authentication
and interaction with the Checks API we are not plugging on a complete webhook
demo, you could use what you have learned from here and follow the [official
Github tutorial](https://docs.github.com/en/developers/apps/guides/creating-ci-tests-with-the-checks-api)

[These runs were created using this demo.](https://github.com/brandonstubbs/bb-github-app/runs/7220694563)

## Prerequisites
You will need two things: \
A repository and a commit on that repository which we will create the check runs
on. \
A Github Application and private key to use for authentication.

### Github Application Setup
#### Create Application
To create a Github Application you need to do the following:
- In your Github Settings, navigate to: Developer settings -> Github Apps.
- Click "New Github App"
  - Choose a unique Github App name. For this example I am using `bb checks`
  - Homepage URL. Link to your application webpage. For this example I am using
    `https://babashka.org/`
  - Webhook -> active. Disable this, not in scope for this demo.
  - Repository Permissions -> Enable `Read and write` for `Checks`.
  - Click "Create GitHub App"
  - Take note of the `App ID` under the about section, you will need to copy
    this into the code.

#### Generate Application Private Key
To create a Github Application private key, you will need to do the following:
- In your Github Settings, navigate to: Developer settings -> Github Apps.
- Click "Edit" next to the Github app your are using for this.
- Scroll down to "Private keys"
- click "Generate"
- Take note of this downloaded pem, you will need to copy the location of this
  into the code.

#### Install Github app to your account
You have created a Github application that can be used by other accounts, now
you just need to install it into yours.
- In your Github Settings, navigate to: Developer settings -> Github Apps.
- Click "Edit" next to the Github app your are using for this.
- Click "Public page" in the sidebar.
- Click "Install".


## Run Example
Statup a Babashka REPL! Open [github_app.clj](github_app.clj) and navigate to
the [Rich Comment Block](https://betweentwoparens.com/blog/rich-comment-blocks/#rich-comment).
Change the variables under "change these" and step through the examples.
