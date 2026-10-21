package academy.tochkavhoda.competition.daoimpl;

import academy.tochkavhoda.competition.dao.ParticipantDao;
import academy.tochkavhoda.competition.database.Database;
import academy.tochkavhoda.competition.model.Participant;

public class ParticipantDaoImpl implements ParticipantDao {
    private final Database database = Database.getInstance();

    @Override
    public void insert(Participant participant) {
        database.insertParticipant(participant);
    }
}
