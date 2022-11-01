package org.cis120;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class ServerModelTest {
    private ServerModel model;

    /**
     * Before each test, we initialize model to be
     * a new ServerModel (with all new, empty state)
     */
    @BeforeEach
    public void setUp() {
        // We initialize a fresh ServerModel for each test
        model = new ServerModel();
    }

    /**
     * Here is an example test that checks the functionality of your
     * changeNickname error handling. Each line has commentary directly above
     * it which you can use as a framework for the remainder of your tests.
     */
    @Test
    public void testInvalidNickname() {
        // A user must be registered before their nickname can be changed,
        // so we first register a user with an arbitrarily chosen id of 0.
        model.registerUser(0);

        // We manually create a Command that appropriately tests the case
        // we are checking. In this case, we create a NicknameCommand whose
        // new Nickname is invalid.
        Command command = new NicknameCommand(0, "User0", "!nv@l!d!");

        // We manually create the expected Broadcast using the Broadcast
        // factory methods. In this case, we create an error Broadcast with
        // our command and an INVALID_NAME error.
        Broadcast expected = Broadcast.error(
                command, ServerResponse.INVALID_NAME
        );

        // We then get the actual Broadcast returned by the method we are
        // trying to test. In this case, we use the updateServerModel method
        // of the NicknameCommand.
        Broadcast actual = command.updateServerModel(model);

        // The first assertEquals call tests whether the method returns
        // the appropriate Broadcast.
        assertEquals(expected, actual, "Broadcast");

        // We also want to test whether the state has been correctly
        // changed.In this case, the state that would be affected is
        // the user's Collection.
        Collection<String> users = model.getRegisteredUsers();

        // We now check to see if our command updated the state
        // appropriately. In this case, we first ensure that no
        // additional users have been added.
        assertEquals(1, users.size(), "Number of registered users");

        // We then check if the username was updated to an invalid value
        // (it should not have been).
        assertTrue(users.contains("User0"), "Old nickname still registered");

        // Finally, we check that the id 0 is still associated with the old,
        // unchanged nickname.
        assertEquals(
                "User0", model.getNickname(0),
                "User with id 0 nickname unchanged"
        );
    }

    /*
     * Your TAs will be manually grading the tests that you write below this
     * comment block. Don't forget to test the public methods you have added to
     * your ServerModel class, as well as the behavior of the server in
     * different scenarios.
     * You might find it helpful to take a look at the tests we have already
     * provided you with in Task4Test, Task3Test, and Task5Test.
     */

    // Task 3
    @Test
    public void testNickInChannels() {
        model.registerUser(0);
        model.registerUser(1);
        Command channel1 = new CreateCommand(0, "User0", "1", false);
        Command channel2 = new CreateCommand(0, "User0", "2", false);
        Command join = new JoinCommand(1, "User1", "2");
        Command command = new NicknameCommand(0, "User0", "cis120");

        channel1.updateServerModel(model);
        channel2.updateServerModel(model);
        join.updateServerModel(model);
        command.updateServerModel(model);

        Set<String> users = new TreeSet<String>();
        users.add("User1");
        users.add("cis120");
        assertEquals(model.getUsersInChannel("2"), users, "nick changed in channel");
    }

    @Test
    public void testOwnerDeregisterMultipleChannels() {
        model.registerUser(0);
        model.registerUser(1);
        model.registerUser(2);
        Command channel1 = new CreateCommand(0, "User0", "1", false);
        Command join1 = new JoinCommand(1, "User1", "1");
        Command join2 = new JoinCommand(2, "User2", "1");
        Command channel2 = new CreateCommand(0, "User0", "2", false);

        channel1.updateServerModel(model);
        channel2.updateServerModel(model);
        join1.updateServerModel(model);
        join2.updateServerModel(model);

        model.deregisterUser(0);
        assertTrue(model.getChannels().isEmpty(), "channel removed by deregistered owner");
    }

    @Test
    public void testDeregisterNotify() {
        model.registerUser(0);
        model.registerUser(1);
        model.registerUser(2);
        Command channel1 = new CreateCommand(0, "User0", "1", false);
        Command join1 = new JoinCommand(1, "User1", "1");
        Command join2 = new JoinCommand(2, "User2", "1");
        Command channel2 = new CreateCommand(0, "User0", "2", false);

        channel1.updateServerModel(model);
        channel2.updateServerModel(model);
        join1.updateServerModel(model);
        join2.updateServerModel(model);

        Set<String> users = new TreeSet<String>();
        users.add("User0");
        users.add("User1");

        assertEquals(
                model.deregisterUser(2), Broadcast.disconnected("User2", users),
                "userNotifyList is the same as users"
        );
    }

    // Test 4
    @Test
    public void testLeaveOwner() {
        model.registerUser(0);
        model.registerUser(1);
        Command channel = new CreateCommand(0, "User0", "java", false);
        channel.updateServerModel(model);
        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);

        Command leave = new LeaveCommand(0, "User0", "java");
        leave.updateServerModel(model);
        assertTrue(model.getChannels().isEmpty(), "No Channel Left");
    }

    @Test
    public void testCreateChannelAlreadyExistsError() {
        model.registerUser(0);
        Command create = new CreateCommand(0, "User0", "java", false);
        Command create1 = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Broadcast expected = Broadcast.error(create1, ServerResponse.CHANNEL_ALREADY_EXISTS);
        assertEquals(expected, create1.updateServerModel(model), "Duplicate");
    }

    @Test
    public void testJoinChannelUserNotInChannelError() {
        model.registerUser(0);

        Command join = new JoinCommand(0, "User0", "java");
        Broadcast expected = Broadcast.error(join, ServerResponse.NO_SUCH_CHANNEL);
        assertEquals(expected, join.updateServerModel(model), "No Channel Error");
    }

    @Test
    public void testJoinChannelPrivateChannelError() {
        model.registerUser(0);

        Command channel = new CreateCommand(0, "User0", "java", true);
        channel.updateServerModel(model);

        Command join = new JoinCommand(0, "User0", "java");
        Broadcast expected = Broadcast.error(join, ServerResponse.JOIN_PRIVATE_CHANNEL);
        assertEquals(expected, join.updateServerModel(model), "Join Private Channel Error");
    }

    @Test
    public void testSendMessageNoSuchChannelError() {
        model.registerUser(0);

        Command mesg = new MessageCommand(0, "User0", "java", "hi");
        Broadcast expected = Broadcast.error(mesg, ServerResponse.NO_SUCH_CHANNEL);
        assertEquals(expected, mesg.updateServerModel(model), "No Such Channel Error");
    }

    @Test
    public void testSendMessageNoUserChannelError() {
        model.registerUser(0);
        model.registerUser(1);
        model.registerUser(2);
        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Command join = new JoinCommand(1, "User1", "java");
        join.updateServerModel(model);

        Command mesg = new MessageCommand(2, "User2", "java", "hi");
        Broadcast expected = Broadcast.error(mesg, ServerResponse.USER_NOT_IN_CHANNEL);
        assertEquals(expected, mesg.updateServerModel(model), "User Not In Channel Error");
    }

    // Test 5
    @Test
    public void testInviteNotifyChannelUsers() {
        model.registerUser(0);
        model.registerUser(1);
        model.registerUser(2);
        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);
        Command inviteValid1 = new InviteCommand(0, "User0", "java", "User1");
        inviteValid1.updateServerModel(model);

        Set<String> users = new TreeSet<String>();
        users.add("User0");
        users.add("User1");

        Broadcast expected = Broadcast.names(inviteValid1, users, "User0");
        assertEquals(expected, inviteValid1.updateServerModel(model), "Notified User0, User1");

        Command inviteValid2 = new InviteCommand(0, "User0", "java", "User2");
        inviteValid2.updateServerModel(model);

        users.add("User2");

        Broadcast expected1 = Broadcast.names(inviteValid2, users, "User0");
        assertEquals(
                expected1, inviteValid2.updateServerModel(model), "Notified User0, User1, User2"
        );
    }

    @Test
    public void testInviteOwnerItself() {
        model.registerUser(0);
        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);
        Command inviteValid = new InviteCommand(0, "User0", "java", "User0");
        inviteValid.updateServerModel(model);

        Set<String> users = new TreeSet<String>();
        users.add("User0");

        Broadcast expected = Broadcast.names(inviteValid, users, "User0");
        assertEquals(expected, inviteValid.updateServerModel(model), "Notified User0");
    }

    @Test
    public void testInviteNoUserError() {
        model.registerUser(0);
        model.registerUser(1);
        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);
        Command inviteInvalid = new InviteCommand(0, "User0", "java", "User2");
        inviteInvalid.updateServerModel(model);

        Broadcast expected = Broadcast.error(inviteInvalid, ServerResponse.NO_SUCH_USER);
        assertEquals(expected, inviteInvalid.updateServerModel(model));
    }

    @Test
    public void testInviteNoChannelError() {
        model.registerUser(0);
        model.registerUser(1);
        Command inviteInvalid = new InviteCommand(0, "User0", "java", "User1");
        inviteInvalid.updateServerModel(model);

        Broadcast expected = Broadcast.error(inviteInvalid, ServerResponse.NO_SUCH_CHANNEL);
        assertEquals(expected, inviteInvalid.updateServerModel(model));
    }

    @Test
    public void testInvitePublicError() {
        model.registerUser(0);
        model.registerUser(1);
        Command create = new CreateCommand(0, "User0", "java", false);
        create.updateServerModel(model);
        Command inviteInvalid = new InviteCommand(0, "User0", "java", "User1");
        inviteInvalid.updateServerModel(model);

        Broadcast expected = Broadcast
                .error(inviteInvalid, ServerResponse.INVITE_TO_PUBLIC_CHANNEL);
        assertEquals(expected, inviteInvalid.updateServerModel(model));
    }

    @Test
    public void testJoinPrivateChannelError() {
        model.registerUser(0);
        model.registerUser(1);
        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);

        Command join = new JoinCommand(1, "User1", "java");
        Broadcast expected = Broadcast.error(join, ServerResponse.JOIN_PRIVATE_CHANNEL);
        assertEquals(expected, join.updateServerModel(model), "Error Join Private Channel");
    }

    @Test
    public void testKickOwnChannel() {
        model.registerUser(0);

        // this command will create a channel called "java" with "User0" (with id = 0)
        // as the owner
        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);

        Command kick = new KickCommand(0, "User0", "java", "User0");
        kick.updateServerModel(model);

        assertTrue(model.getChannels().isEmpty(), "User kicked itself out of the channel");
    }

    @Test
    public void testKickNoUserError() {
        model.registerUser(0);

        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);

        Command kick = new KickCommand(0, "User0", "java", "User1");
        kick.updateServerModel(model);

        Broadcast expected = Broadcast.error(kick, ServerResponse.NO_SUCH_USER);
        assertEquals(expected, kick.updateServerModel(model), "No Such User Exists");
    }

    @Test
    public void testKickNoChannelError() {
        model.registerUser(0);
        model.registerUser(1);

        Command kick = new KickCommand(0, "User0", "java", "User1");
        kick.updateServerModel(model);

        Broadcast expected = Broadcast.error(kick, ServerResponse.NO_SUCH_CHANNEL);
        assertEquals(expected, kick.updateServerModel(model), "No Such Channel Exists");
    }

    @Test
    public void testKickPublicInviteError() {
        model.registerUser(0);
        model.registerUser(1);
        model.registerUser(2);

        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);

        Command inviteValid = new InviteCommand(0, "User0", "java", "User1");
        inviteValid.updateServerModel(model);

        Command kick = new KickCommand(0, "User0", "java", "User2");
        kick.updateServerModel(model);

        Broadcast expected = Broadcast.error(kick, ServerResponse.USER_NOT_IN_CHANNEL);
        assertEquals(expected, kick.updateServerModel(model), "No Such User In Channel");
    }

    @Test
    public void testKickNotOwnerError() {
        model.registerUser(0);
        model.registerUser(1);

        Command create = new CreateCommand(0, "User0", "java", true);
        create.updateServerModel(model);

        Command inviteValid = new InviteCommand(0, "User0", "java", "User1");
        inviteValid.updateServerModel(model);

        Command kick = new KickCommand(1, "User1", "java", "User0");
        kick.updateServerModel(model);

        Broadcast expected = Broadcast.error(kick, ServerResponse.USER_NOT_OWNER);
        assertEquals(expected, kick.updateServerModel(model), "Not Owner Kicking User");
    }
}