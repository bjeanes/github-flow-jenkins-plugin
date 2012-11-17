package com.bjeanes;

import hudson.Extension;

import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import hudson.model.TaskListener;

import hudson.plugins.git.util.BuildData;
import hudson.scm.SCM;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * User: bjeanes
 * Date: 11/17/12
 * Time: 1:04 AM
 */
public class GitHubFlow {

    @Extension
    public static class Listener extends RunListener<Run> {
        @Override public void onStarted(Run r,   TaskListener listener) { GitHub.notify(r); }
        @Override public void onCompleted(Run r, TaskListener listener) { GitHub.notify(r); }
        @Override public void onDeleted(Run r)                          { GitHub.notify(r, false); }
    }

    public static class GitHub {
        private static final GitHubClient client = getGitHubClient();
        private static final CommitService commitService = new CommitService(GitHub.client);

        private static final String ERROR   = "error";
        private static final String SUCCESS = "success";
        private static final String FAILURE = "failure";
        private static final String PENDING = "pending";

        private static final HashMap<Result, String> STATUSES;

        static {
            STATUSES = new HashMap<Result, String>();
            STATUSES.put(Result.FAILURE, FAILURE);
            STATUSES.put(Result.UNSTABLE, FAILURE);
            STATUSES.put(Result.NOT_BUILT, ERROR);
            STATUSES.put(Result.ABORTED, ERROR);
            STATUSES.put(Result.SUCCESS, SUCCESS);
            STATUSES.put(null, PENDING);
        }

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
            } catch(IOException e) {
            } catch(IllegalArgumentException e) {
            }
        }

        private static IRepositoryIdProvider repository(Run run) {
            Job job = run.getParent();
            return new IRepositoryIdProvider() {
                public String generateId() {
                // FIXME: Get from job SCM repo?
                return "bjeanes/github-flow-jenkins-plugin";
                }
            };
        };

        private static String sha(Run run) {
            List<BuildData> buildDataList = run.getActions(BuildData.class);
            if(buildDataList.size() > 0) {
                return buildDataList
                        .get(0)
                        .lastBuild
                        .revision
                        .getSha1String();
            } else {
                return null;
            }
        }

        private static CommitStatus status(Run r, boolean linkable) {
            Result result = r.getResult();

            CommitStatus status = new CommitStatus();
            status.setDescription(r.getDescription());
            status.setState(STATUSES.get(result));

            if(linkable) {
                String url = r.getUrl();
                status.setUrl(url);
            }

            return status;
        }
    }
}
