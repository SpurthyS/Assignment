package controller;

/**
 * Created by Spurthy on 2/27/2015.
 */

import domain.*;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.MyStringRandomGen;

import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;


@RestController
public class ModeratorController {



    private ModeratorRepository moderatorRepository = new ModeratorRepository();
    private Map<Integer, List<String>> moderatorIdToPollIdsMap  = new HashMap<Integer, List<String>>();
    private PollRepository pollRepository = new PollRepository();
    private final AtomicLong counter = new AtomicLong();
    //DateFormat df = DateFormat.getDateTimeInstance (DateFormat.MEDIUM, DateFormat.MEDIUM, new Locale("en", "EN"));
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    String formattedDate = df.format (new Date ());
    private Moderator nullModerator = new Moderator();

    public boolean checkAuthorizationDetail(String authorizationDetail)
            throws UnsupportedEncodingException {
        String[] authorizationDetailArray = authorizationDetail.split(" ");
        byte[] decodedString = Base64.decodeBase64(authorizationDetailArray[1]);
        String authorizationString = new String(decodedString, "UTF-8");
        if (authorizationString.indexOf(":") > 0) {
            String[] credentials = authorizationString.split(":");
            String username = credentials[0];
            String password = credentials[1];
            if (username.equals("foo") && password.equals("bar"))
                return true;
        }
        return false;
    }


    //-------------1-------------------
    @RequestMapping(value ="api/v1/moderators", method = RequestMethod.POST)
    public ResponseEntity<Moderator> moderators(@RequestBody @Valid Moderator moderator ){


            moderator.setCreated_at(formattedDate);
            moderator.setId((int) counter.incrementAndGet());
            moderatorRepository.addModerator(moderator);

            return new ResponseEntity<Moderator>(moderator, HttpStatus.OK);
        }


    //-------------2-------------------
    @RequestMapping(value ="api/v1/moderators/{id}", method = RequestMethod.GET)
    public  ResponseEntity<Moderator> getModerator(@PathVariable Integer id) {
        return new ResponseEntity<Moderator>(moderatorRepository.findModeratorInList(id),HttpStatus.OK) ;
    }

    //-------------3-------------------
    @RequestMapping(value ="api/v1/moderators/{id}",method = RequestMethod.PUT )
    public Object updateModerator(@PathVariable Integer id,@RequestBody Moderator mod, String authentication1) throws UnsupportedEncodingException   {


            Moderator prevMod = moderatorRepository.findModeratorInList(id);
            prevMod.setEmail(mod.getEmail());
            prevMod.setPassword(mod.getPassword());
            moderatorRepository.addModerator(mod);
            return new ResponseEntity<Moderator>(prevMod, HttpStatus.OK);


    }

    //-------------4-------------------
    @RequestMapping(value ="api/v1/moderators/{id}/polls", method = RequestMethod.POST)
    public ResponseEntity<Poll> postPoll(@PathVariable Integer id,@RequestBody Poll poll) {
        poll.setId(MyStringRandomGen.generateRandomString().toUpperCase());
        moderatorRepository.findModeratorInList(id).getPolls().add(poll);
        pollRepository.addPoll(poll);

        if(moderatorIdToPollIdsMap.get(id) == null){
            List<String> pollIdList = new ArrayList<String>();
            pollIdList.add(poll.getId());
            moderatorIdToPollIdsMap.put(id,pollIdList);
        } else {
            (moderatorIdToPollIdsMap.get(id)).add(poll.getId());
        }
        System.out.println(moderatorIdToPollIdsMap.toString());
        return new ResponseEntity<Poll>(poll, HttpStatus.OK);
    }

    //-------------5-------------------
    @RequestMapping(value ="api/v1/polls/{id}", method = RequestMethod.GET)
    public  ResponseEntity<PollWithoutResults> getPollWithoutResults(@PathVariable String id) {
        return new ResponseEntity<PollWithoutResults>(pollRepository.findPollWoResultInList(id), HttpStatus.OK);
    }

    //-------------6-------------------
    @RequestMapping(value ="api/v1/moderators/{moderator_id}/polls/{poll_id}", method = RequestMethod.GET)
    public  ResponseEntity<Poll> getPollWithResults( @PathVariable String moderator_id, @PathVariable String poll_id ) {
        Poll returnPoll = null;
        List<Poll> pollList = moderatorRepository.findModeratorInList(Integer.valueOf(moderator_id)).getPolls();
        for(Poll pollWithId : pollList){
            if(pollWithId.getId().equals(poll_id)){
                returnPoll=pollWithId;
            }
        }
        return new ResponseEntity<Poll>(returnPoll, HttpStatus.OK);

    }
    //-------------7-------------------

    @RequestMapping(value ="api/v1/moderators/{moderator_id}/polls", method = RequestMethod.GET)
    public  ResponseEntity<List<Poll>> getPollWithResults( @PathVariable String moderator_id ) {
        List<Poll> pollList = moderatorRepository.findModeratorInList(Integer.valueOf(moderator_id)).getPolls();
        return new ResponseEntity<List<Poll>>(pollList, HttpStatus.OK);

    }

    //-------------8-------------------
    @RequestMapping(value ="api/v1/moderators/{moderator_id}/polls/{poll_id}", method = RequestMethod.DELETE)
    public ResponseEntity deletePoll( @PathVariable Integer moderator_id, @PathVariable String poll_id ) {
        Poll returnPoll = null;
        List<Poll> pollList = moderatorRepository.findModeratorInList(moderator_id).getPolls();
        for (Poll pollWithId : pollList) {
            if (pollWithId.getId().equals(poll_id)) {
                returnPoll = pollWithId;
            }
        }
        if (returnPoll == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        } else {
            moderatorRepository.findModeratorInList(moderator_id).getPolls().remove(returnPoll);
            pollRepository.removePoll(returnPoll);
            (moderatorIdToPollIdsMap.get(moderator_id)).remove(poll_id);
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }
    }

       //-------------9-------------------
        @RequestMapping(value = "api/v1/polls/{poll_id}", method = RequestMethod.PUT)
        public  ResponseEntity castVote(@PathVariable String poll_id, @RequestParam Integer choice) {
            Poll inputPoll = pollRepository.findPollInList(poll_id);
            //int resultCount = inputPoll.getResultsForIndex(choice);
            inputPoll.setResultsForIndex(choice);
            Integer moderatorId = null;

            for(Map.Entry<Integer, List<String>> entry : moderatorIdToPollIdsMap.entrySet()){
                for(String pollIdValue : entry.getValue()){
                    System.out.println(pollIdValue);
                   if(poll_id.equals(pollIdValue)){
                       moderatorId = entry.getKey();
                   }
                }
            }
            System.out.println(moderatorId);
            moderatorRepository.updatePoll(moderatorId,inputPoll);
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }

    }
















