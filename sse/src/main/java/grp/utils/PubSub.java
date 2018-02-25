package grp.utils;

import pl.touk.throwing.ThrowingConsumer;

import java.util.concurrent.Flow;
import java.util.function.BiConsumer;
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


    public static class DefaultSubscriber<T> implements Flow.Subscriber<T> {
        Flow.Subscription subscription;

        final ThrowingConsumer<T,Exception> bc;

        public static <T> Flow.Subscriber<T> subscriber(ThrowingConsumer<T,Exception> bc) {
            return new DefaultSubscriber<>(bc);
        }

        private DefaultSubscriber(ThrowingConsumer<T,Exception> bc) {
            this.bc = bc;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(T item) {
            try {
                bc.accept(item);
            } catch (Exception e) {
                subscription.cancel();
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onComplete() {

        }
    }


}
