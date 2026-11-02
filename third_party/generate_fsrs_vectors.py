import json, random, sys
from datetime import datetime, timedelta, timezone
from fsrs import Scheduler, Card, Rating, State
random.seed(20261102)
sch = Scheduler(enable_fuzzing=False)
base = datetime(2024, 1, 15, 8, 30, 0, 0, timezone.utc)
out = []
EPOCH = datetime(1970,1,1,tzinfo=timezone.utc)
ms = lambda d: int((d-EPOCH).total_seconds()*1000)
for seq in range(120):
    card = Card(card_id=seq+1, due=base)
    now = base
    steps = []
    for i in range(random.randint(3, 25)):
        mode = random.random()
        if i == 0: pass
        elif mode < 0.55: now = card.due
        elif mode < 0.75: now = card.due + timedelta(days=random.randint(0, 40), minutes=random.randint(0, 1400))
        elif mode < 0.9: now = now + timedelta(minutes=random.randint(0, 600))
        else: now = now + timedelta(days=random.randint(1, 400))
        rating = random.choices([1,2,3,4],[0.18,0.17,0.5,0.15])[0]
        card, _ = sch.review_card(card, Rating(rating), now)
        steps.append(dict(reviewedAt=ms(now), rating=rating, state=int(card.state), step=card.step,
                          stability=card.stability, difficulty=card.difficulty, due=ms(card.due),
                          retrievabilityAtDue=sch.get_card_retrievability(card, card.due)))
    out.append(dict(startAt=ms(base), steps=steps))
json.dump(dict(generator="py-fsrs 6.3.2", parameters=list(sch.parameters), sequences=out), open(sys.argv[1], "w"))
print(len(out), sum(len(s['steps']) for s in out))
