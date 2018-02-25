package grp.utils;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class PubSub {
    public static <T> Flow.Subscriber<T> oneTimeSubscriber(final Consumer<T> consumer) {
        return new Flow.Subscriber<T>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(T item) {
                consumer.accept(item);
                subscription.cancel();

            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {

            }
        };
    }

}
