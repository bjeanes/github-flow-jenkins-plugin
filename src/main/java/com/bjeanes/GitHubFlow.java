package com.bjeanes;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import hudson.model.TaskListener;

import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.IOException;

/**
 * User: bjeanes
 * Date: 11/17/12
 * Time: 1:04 AM
 */
public class GitHubFlow {

    @Extension
    class Listener extends RunListener<Run> {
        @Override public void onStarted(Run r,   TaskListener listener) { GitHub.notify(r); }
        @Override public void onCompleted(Run r, TaskListener listener) { GitHub.notify(r); }
        @Override public void onDeleted(Run r)                          { GitHub.notify(r, false); }
    }

    static class GitHub {
        private static final GitHubClient client = getGitHubClient();
        private static final CommitService commitService = new CommitService(GitHub.client);

        public static GitHubClient getGitHubClient() {
            // FIXME: Fetch this from config to support GHE
            String hostname = null;

            if(null == hostname) {
                return new GitHubClient();
            }
            else {
                // TODO: Configuration should provide whole URL and we
                // extract out these components
                return new GitHubClient(hostname, 443, "https");
            }
        }

        public static void notify(Run r) {
            GitHub.notify(r, true); }

        public static void notify(Run r, boolean linkable) {
            try {
                commitService.createStatus(repository(r), sha(r), status(r, linkable));
            } catch (IOException e) { }
        }

        private static IRepositoryIdProvider repository(Run run) {
            Job job = run.getParent();
            return new IRepositoryIdProvider() {
                public String generateId() {
                    // FIXME: Get from job SCM repo?
                    return "bjeanes/repo";
                }
            };
        };

        private static String sha(Run r) {
            return "";
        }

        private static CommitStatus status(Run r, boolean linkable) {
            Result result = r.getResult();

            CommitStatus status = new CommitStatus();
            status.setDescription(r.getDescription());

            if(null != result) status.setState(result.toString());

            if(linkable) {
                String url = r.getUrl();
                status.setUrl(url);
            }

            return status;
        }
    }
}
