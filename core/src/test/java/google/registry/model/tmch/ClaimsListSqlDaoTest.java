// Copyright 2019 The Nomulus Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package google.registry.model.tmch;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth8;
import google.registry.persistence.transaction.JpaTestRules;
import google.registry.persistence.transaction.JpaTestRules.JpaIntegrationWithCoverageExtension;
import google.registry.testing.DatastoreEntityExtension;
import google.registry.testing.FakeClock;
import javax.persistence.PersistenceException;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Unit tests for {@link ClaimsListSqlDao}. */
public class ClaimsListSqlDaoTest {

  private final FakeClock fakeClock = new FakeClock();

  @RegisterExtension
  final JpaIntegrationWithCoverageExtension jpa =
      new JpaTestRules.Builder().withClock(fakeClock).buildIntegrationWithCoverageExtension();

  @RegisterExtension
  @Order(value = 1)
  final DatastoreEntityExtension datastoreEntityExtension = new DatastoreEntityExtension();

  @Test
  void save_insertsClaimsListSuccessfully() {
    ClaimsListShard claimsList =
        ClaimsListShard.create(
            fakeClock.nowUtc(), ImmutableMap.of("label1", "key1", "label2", "key2"));
    ClaimsListSqlDao.save(claimsList);
    ClaimsListShard insertedClaimsList = ClaimsListSqlDao.get().get();
    assertClaimsListEquals(claimsList, insertedClaimsList);
    assertThat(insertedClaimsList.getCreationTimestamp()).isEqualTo(fakeClock.nowUtc());
  }

  @Test
  void save_fail_duplicateId() {
    ClaimsListShard claimsList =
        ClaimsListShard.create(
            fakeClock.nowUtc(), ImmutableMap.of("label1", "key1", "label2", "key2"));
    ClaimsListSqlDao.save(claimsList);
    ClaimsListShard insertedClaimsList = ClaimsListSqlDao.get().get();
    assertClaimsListEquals(claimsList, insertedClaimsList);
    // Save ClaimsList with existing revisionId should fail because revisionId is the primary key.
    assertThrows(PersistenceException.class, () -> ClaimsListSqlDao.save(insertedClaimsList));
  }

  @Test
  void save_claimsListWithNoEntries() {
    ClaimsListShard claimsList = ClaimsListShard.create(fakeClock.nowUtc(), ImmutableMap.of());
    ClaimsListSqlDao.save(claimsList);
    ClaimsListShard insertedClaimsList = ClaimsListSqlDao.get().get();
    assertClaimsListEquals(claimsList, insertedClaimsList);
    assertThat(insertedClaimsList.getLabelsToKeys()).isEmpty();
  }

  @Test
  void getCurrent_returnsEmptyListIfTableIsEmpty() {
    Truth8.assertThat(ClaimsListSqlDao.get()).isEmpty();
  }

  @Test
  void getCurrent_returnsLatestClaims() {
    ClaimsListShard oldClaimsList =
        ClaimsListShard.create(
            fakeClock.nowUtc(), ImmutableMap.of("label1", "key1", "label2", "key2"));
    ClaimsListShard newClaimsList =
        ClaimsListShard.create(
            fakeClock.nowUtc(), ImmutableMap.of("label3", "key3", "label4", "key4"));
    ClaimsListSqlDao.save(oldClaimsList);
    ClaimsListSqlDao.save(newClaimsList);
    assertClaimsListEquals(newClaimsList, ClaimsListSqlDao.get().get());
  }

  private void assertClaimsListEquals(ClaimsListShard left, ClaimsListShard right) {
    assertThat(left.getRevisionId()).isEqualTo(right.getRevisionId());
    assertThat(left.getTmdbGenerationTime()).isEqualTo(right.getTmdbGenerationTime());
    assertThat(left.getLabelsToKeys()).isEqualTo(right.getLabelsToKeys());
  }
}
