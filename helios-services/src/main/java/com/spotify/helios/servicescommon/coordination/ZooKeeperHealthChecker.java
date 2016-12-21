/*-
 * -\-\-
 * Helios Services
 * --
 * Copyright (C) 2016 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.helios.servicescommon.coordination;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

public class ZooKeeperHealthChecker extends HealthCheck
    implements Managed, PathChildrenCacheListener {

  private final PathChildrenCache cache;
  private AtomicReference<String> reasonString = new AtomicReference<String>("UNKNOWN");

  public ZooKeeperHealthChecker(final ZooKeeperClient zooKeeperClient, final String path) {
    super();
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    this.cache = new PathChildrenCache(zooKeeperClient.getCuratorFramework(), path, true, false,
        scheduler);
  }

  private void setState(String newState) {
    if ((reasonString.get() == null) != (newState == null)) {
      reasonString.set(newState);
    }
  }

  // TODO (mbrown): couldn't this be done with a client.getConnectionStateListenable() listener?
  // this is keeping everything under /status/hosts cached
  @Override
  public void childEvent(CuratorFramework curator, PathChildrenCacheEvent event)
      throws Exception {
    switch (event.getType()) {
      case INITIALIZED:
      case CONNECTION_RECONNECTED:
      case CHILD_ADDED:
      case CHILD_REMOVED:
      case CHILD_UPDATED:
        // If we get any of these, clearly we're connected.
        setState(null);
        break;

      case CONNECTION_LOST:
        setState("CONNECTION_LOST");
        break;
      case CONNECTION_SUSPENDED:
        setState("CONNECTION_SUSPENDED");
        break;
      default:
        throw new IllegalStateException("Unrecognized event " + event.getType());
    }
  }

  @Override
  public void start() throws Exception {
    cache.start();
    cache.getListenable().addListener(this);
  }

  @Override
  public void stop() throws Exception {
  }

  @Override
  protected Result check() throws Exception {
    if (reasonString.get() == null) {
      return Result.healthy();
    } else {
      return Result.unhealthy(reasonString.get());
    }
  }
}
