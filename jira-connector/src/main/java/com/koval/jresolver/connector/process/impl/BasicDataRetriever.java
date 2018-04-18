package com.koval.jresolver.connector.process.impl;

import java.util.Iterator;

import com.atlassian.jira.rest.client.domain.BasicIssue;
import com.koval.jresolver.connector.bean.JiraIssue;
import com.koval.jresolver.connector.bean.JiraSearchResult;
import com.koval.jresolver.connector.client.JiraClient;
import com.koval.jresolver.connector.deliver.DataConsumer;
import com.koval.jresolver.connector.process.DataRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BasicDataRetriever implements DataRetriever {

  private static final Logger LOGGER = LoggerFactory.getLogger(BasicDataRetriever.class);

  private JiraClient jiraClient;
  private DataConsumer dataConsumer;
  private String jql;
  private int maxResults;
  private int startAt;
  private int delayAfterEveryRequest;
  private int maxIssues;
  private double status = 0.0;
  private boolean isComplete = false;

  public BasicDataRetriever(JiraClient jiraClient, DataConsumer dataConsumer, String jql, int maxResults, int startAt, int delayAfterEveryRequest, int maxIssues) {
    this.jiraClient = jiraClient;
    this.dataConsumer = dataConsumer;
    this.jql = jql;
    this.maxResults = maxResults;
    this.startAt = startAt;
    this.delayAfterEveryRequest = delayAfterEveryRequest;
    this.maxIssues = maxIssues;
  }

  @Override
  public void start() {
    status = 0.0;
    isComplete = false;
    LOGGER.info("Receiving data started.");
    while (!isComplete) {
      JiraSearchResult searchResult = jiraClient.searchByJql(jql, maxResults, startAt);

      Iterator<BasicIssue> issueIterator = searchResult.getIssues().iterator();

      if (!issueIterator.hasNext()) {
        LOGGER.info("All data was collected. Stop receiving.");
        break;
      }

      int index = 0;
      while (issueIterator.hasNext()) {
        BasicIssue basicIssue = issueIterator.next();
        JiraIssue issue = jiraClient.getIssueByKey(basicIssue.getKey());
        dataConsumer.consume(issue);
        index++;
        LOGGER.info("Progress: " + (startAt + index) + "/" + searchResult.getTotal() + " Issue: " + issue.getKey());
        if (startAt + index >= maxIssues) {
          LOGGER.info("Max issues number was reached. Stop receiving.");
          isComplete = true;
          break;
        }
      }

      setStatus((double) (startAt + index)/searchResult.getTotal());
      startAt += maxResults;
      delay();
    }
    LOGGER.info("Receiving data stopped.");
  }

  @Override
  public void stop() {
    isComplete = true;
  }

  private void delay() {
    try {
      Thread.sleep(delayAfterEveryRequest);
      LOGGER.info("Waiting for " + delayAfterEveryRequest + "ms ...");
    } catch (InterruptedException ignored) {
    }
  }

  private void setStatus(double status) {
    this.status = status;
  }

  @Override
  public double getStatus() {
    return status;
  }
}
