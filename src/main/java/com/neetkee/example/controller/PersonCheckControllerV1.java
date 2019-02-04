package com.neetkee.example.controller;

import com.neetkee.example.event.PersonCheckInitiated;
import com.neetkee.example.event.PersonChecked;
import com.neetkee.example.model.Person;
import com.neetkee.example.model.PersonCheckResult;
import com.neetkee.example.repository.PersonCheckResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.*;

@RestController
@RequiredArgsConstructor
@Slf4j
//@Log
@RequestMapping("/v1/persons")
public class PersonCheckControllerV1 {

    //Logger log = LoggerFactory.getLogger(PersonCheckController.class);

    private final KafkaTemplate<Integer, PersonCheckInitiated> kafkaTemplate;
    private final PersonCheckResultRepository checkResultRepository;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);


//    @Autowired
//    public void setKafkaTemplate(final KafkaTemplate<Integer, PersonCheckInitiated> kafkaTemplate) {
//        this.kafkaTemplate = kafkaTemplate;
//    }

//    @Autowired
//    public void setPersonCheckResultRepository(final PersonCheckResultRepository checkResultRepository) {
//        this.checkResultRepository = checkResultRepository;
//    }

    @Value("${app.kafka.topic.request}")
    private String requestTopicName;

    @Value("${app.kafka.topic.reply}")
    private String replyTopicName;

    @Value("${app.kafka.polling.timeout-seconds}")
    private int timeoutSeconds;

    @Value("${app.kafka.polling.period-seconds}")
    private int pollingPeriodSeconds;

    @Value("${app.kafka.polling.initial-delay-seconds}")
    private int initialDelaySeconds;

    @Value("${app.kafka.polling.cancel-delay-seconds}")
    private int cancelDelaySeconds;
    //private final String replyTopicName1 = replyTopicName;


    @PostMapping("/check")
    //you may want to try/catch this
    @SneakyThrows({InterruptedException.class, ExecutionException.class, TimeoutException.class})
    public PersonCheckResult checkPerson(@RequestBody Person person) {
        log.info("Started, person: {}", person);
        kafkaTemplate.send(/*"Person.Check.Initiated"*/requestTopicName, person.getId(), new PersonCheckInitiated(person));
        log.debug("Request to check sent, polling results with timeout {} second(s)", timeoutSeconds);
        return pollForCheckResult(person.getId()).get(/*50*/timeoutSeconds, TimeUnit.SECONDS);
    }

    private CompletableFuture<PersonCheckResult> pollForCheckResult(Integer personId) {
        CompletableFuture<PersonCheckResult> checkResultCompletableFuture = new CompletableFuture<>();
        final ScheduledFuture<?> checkResultScheduledFuture = executor.scheduleAtFixedRate(() -> {
            log.info("Checking result for person with id: {}, timeout: {}, initial delay: {}, polling period: {} sec.", personId, timeoutSeconds, initialDelaySeconds, pollingPeriodSeconds);
            Optional<PersonCheckResult> optionalCheckResult = checkResultRepository.findByPersonId(personId);
            optionalCheckResult.ifPresent(checkResultCompletableFuture::complete);
        }, initialDelaySeconds/*1*/, pollingPeriodSeconds/*1*/, TimeUnit.SECONDS);

        //we don't want to run this future indefinitely
        executor.schedule(() -> {
            log.info("Cancelling check for person with id: {}, cancel delay: {} second(s)", personId, cancelDelaySeconds);
            checkResultScheduledFuture.cancel(true);
        }, cancelDelaySeconds/*65*/, TimeUnit.SECONDS);

        //cancel polling when result is received
        checkResultCompletableFuture.whenComplete((personCheckResult, throwable) -> checkResultScheduledFuture.cancel(true));
        return checkResultCompletableFuture;
    }

    @KafkaListener(topics = "Person.Checked" /*replyTopicName*/)
    public void personCheckedReceived(PersonChecked personChecked) {
        log.info("Received personCheckedEvent. Id:{}, CheckResult: {}", personChecked.getPersonId(), personChecked.getCheckResult());
        PersonCheckResult personCheckResult = new PersonCheckResult();
        personCheckResult.setPersonId(personChecked.getPersonId());
        personCheckResult.setCheckResult(personChecked.getCheckResult());
        checkResultRepository.save(personCheckResult);
    }


//    @KafkaListener(topics = "Person.Check.Initiated")
//    public void personCheckInitiatedReceived(PersonCheckInitiated personCheckInitiated) {
//        log.info("Received personCheckInitiated: {}", personCheckInitiated);
//    }

//    @KafkaListener(topics = "${app.kafka.topic.request}")
//    @SendTo
//    public PersonChecked personCheckInitiatedReceived(PersonCheckInitiated personCheckInitiated) {
//        PersonChecked result = new PersonChecked();
//        // TODO work emulation
//        if (personCheckInitiated!=null && personCheckInitiated.getPerson()!=null) {
//
//            result.setPersonId(personCheckInitiated.getPerson().getId());
//
//            /*
//             * Check will be ok, if:
//             * first name length >= 7 OR last name starts with 'A' or id is simple number
//             */
//            boolean checkResult = (
//                    personCheckInitiated.getPerson().getFirstName().length() >= 7 ||
//                    personCheckInitiated.getPerson().getLastName().toLowerCase().startsWith("a") ||
//                    isPrime(personCheckInitiated.getPerson().getId())
//            );
//
//            result.setCheckResult(checkResult);
//            log.info("Done, will return: {}", result);
//            return result;
//
//        } else {
//            throw new IllegalArgumentException("Got null person check initiation data");
//        }
//    }


    //checks whether an int is prime or not.
    private static boolean isPrime(int n) {
        //check if n is a multiple of 2
        if (n%2==0) return false;
        //if not, then just check the odds
        for(int i=3;i*i<=n;i+=2) {
            if(n%i==0)
                return false;
        }
        return true;
    }


}
