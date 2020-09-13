/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.repository;

import alfio.model.poll.Poll;
import alfio.model.support.Array;
import alfio.model.support.JSONData;
import ch.digitalfondue.npjt.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@QueryRepository
public interface PollRepository {


    @Query("insert into poll (title, description, allowed_tags, poll_order, event_id_fk, organization_id_fk) " +
        "values(:title::jsonb, :description::jsonb, :allowedTags::text[], :pollOrder, :eventId, :orgId)")
    @AutoGeneratedKey("id")
    AffectedRowCountAndKey<Long> insert(@Bind("title") @JSONData Map<String, String> title,
                                  @Bind("description") @JSONData Map<String, String> description,
                                  @Bind("allowedTags") @Array List<String> allowedTags,
                                  @Bind("pollOrder") int pollOrder,
                                  @Bind("eventId") int eventId,
                                  @Bind("orgId") int organizationId);

    @Query("update poll set title = :title::jsonb, description = :description::jsonb, allowed_tags = :allowedTags::text[]," +
        " poll_order = :pollOrder where id = :id and event_id_fk = :eventId")
    int update(@Bind("title") @JSONData Map<String, String> title,
               @Bind("description") @JSONData Map<String, String> description,
               @Bind("allowedTags") @Array List<String> allowedTags,
               @Bind("pollOrder") int pollOrder,
               @Bind("id") long pollId,
               @Bind("eventId") int eventId);

    @Query("update poll set status = :status where id = :id and event_id_fk = :eventId")
    int updateStatus(@Bind("status") Poll.PollStatus status, @Bind("eventId") int eventId);

    @Query("select * from poll where event_id_fk = :eventId order by poll_order asc")
    List<Poll> findAllInEvent(@Bind("eventId") int eventId);

    @Query("select * from poll where event_id_fk = :eventId and status = 'OPEN' order by poll_order asc")
    List<Poll> findActiveInEvent(@Bind("eventId") int eventId);

    @Query("select * from poll where event_id_fk = :eventId and id = :pollId")
    Optional<Poll> findOptional(@Bind("pollId") long pollId, @Bind("eventId") int eventId);
}
