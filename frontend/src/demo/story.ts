const easeOut = 'cubic-bezier(0.23, 1, 0.32, 1)';

type Cue = {
  selector: string;
  delay: number;
  duration: number;
  keyframes: Keyframe[];
  easing?: string;
};

function cue(selector: string, delay: number, duration: number, keyframes: Keyframe[], easing = easeOut): Cue {
  return { selector, delay, duration, keyframes, easing };
}

function fade(selector: string, delay: number, duration: number, start: number, end: number) {
  return cue(selector, delay, duration, [{ opacity: start }, { opacity: end }]);
}

export const example = {
  repository: 'orders-service',
  contractFile: 'openapi.yaml',
  method: 'GET',
  endpoint: '/shipments/{id}',
  responseStatus: '200',
  responseType: 'application/json',
  fieldType: 'string',
  removedPath: 'tracking_number',
  addedPath: 'tracking.number',
  javaFile: 'ShipmentClient.java',
  sourceFolder: 'src/main/java',
  contextLine: 41,
  affectedLine: 42,
  oldAccessor: 'trackingNumber()',
  newAccessor: 'tracking().number()',
  pullRequest: 42,
  commit: 'c81a9f2',
  case: 'ShipmentTrackingIT',
  command: './mvnw -B verify',
  number: 'TRK-123',
  branch: 'maintainer/shipment-response',
  target: 'main',
};

export const aperture = { width: 688, height: 684 };
export const duration = 14000;

export const chapters = [
  { from: 0, still: 1460, label: 'Контракт API', text: 'В ответе GET /shipments/{id} поле tracking_number изменилось на вложенное tracking.number.' },
  { from: 2160, still: 2780, label: 'Затронута строка 42', text: 'В ShipmentClient.java исходный клиент читает shipment.trackingNumber(). На тестовом ответе новой структуры номер не сохраняется.' },
  { from: 3800, still: 4460, label: 'Клиент адаптирован', text: 'В строке 42 клиент читает вложенное поле через shipment.tracking().number().' },
  { from: 5750, still: 6760, label: 'Проверка клиента', text: 'Исполнитель повторяет ./mvnw -B verify для исправленного клиента на том же тестовом ответе. Отдельно виден результат проверки исходной версии.' },
  { from: 7860, still: 8560, label: 'TRK-123 сохранён', text: 'В интеграционном тесте ShipmentTrackingIT исправленный клиент сохраняет в заказе ожидаемое значение TRK-123. Обязательные проверки завершаются с кодом 0.' },
  { from: 11120, still: duration, label: 'PR ждёт ревью', text: 'После успешных обязательных проверок тот же вариант кода опубликован в GitHub. Ветка maintainer/shipment-response направлена в main. PR #42 открыт и ждёт ревью.' },
];

const cameraEase = 'cubic-bezier(0.645, 0.045, 0.355, 1)';
const cameraStops = { first: 0, checks: -356, pr: -965 };
const cameraPosition = (y: number) => `translate3d(0, ${y}px, 0)`;

export const cues: Cue[] = [
  // The camera carries everything in world coordinates, with a hold at each subject.
  cue('.a3-world', 0, duration, [
    { offset: 0, transform: cameraPosition(cameraStops.first) },
    { offset: 4600 / duration, transform: cameraPosition(cameraStops.first), easing: cameraEase },
    { offset: 6010 / duration, transform: cameraPosition(cameraStops.checks) },
    { offset: 9410 / duration, transform: cameraPosition(cameraStops.checks), easing: cameraEase },
    { offset: 11310 / duration, transform: cameraPosition(cameraStops.pr) },
    { offset: 1, transform: cameraPosition(cameraStops.pr) },
  ], 'linear'),

  cue('.a3-flat-guide', 80, 960, [
    { opacity: 0, offset: 0 },
    { opacity: 1, offset: .26 },
    { opacity: 1, offset: .54 },
    { opacity: 0, offset: 1 },
  ], 'ease'),
  cue('.a3-yaml-number', 470, 790, [
    { transform: 'translate(0, 0)', offset: 0, easing: cameraEase },
    { transform: 'translate(-4px, 29px)', offset: .52, easing: easeOut },
    { transform: 'translate(calc(32px - 9ch), 69px)', offset: 1 },
  ], 'linear'),
  fade('.a3-underscore', 525, 245, 1, 0),
  fade('.a3-yaml-old-type', 570, 190, 1, 0),
  fade('.a3-root-colon', 735, 225, 0, 1),
  fade('.a3-yaml-new-type', 843, 285, 0, 1),
  fade('.a3-yaml-nested', 975, 255, 0, 1),
  fade('.a3-yaml-leaf', 1120, 220, 0, 1),
  fade('.a3-yaml-scalar', 1170, 275, 0, 1),
  cue('.a3-schema-previous', 1260, 350, [
    { opacity: 0, transform: 'translateX(5px)' }, { opacity: 1, transform: 'translateX(0)' },
  ]),

  cue('.a3-trace-ink', 1470, 780, [
    { opacity: 0, strokeDashoffset: '1px', offset: 0 },
    { opacity: 1, strokeDashoffset: '.92px', offset: .13 },
    { opacity: 1, strokeDashoffset: '0px', offset: 1 },
  ], 'linear'),
  fade('.a3-trace-base', 2080, 240, 0, 1),
  cue('.a3-java-marked', 2160, 1490, [
    { opacity: 0, offset: 0, easing: easeOut },
    { opacity: 1, offset: .18 },
    { opacity: 1, offset: .72, easing: easeOut },
    { opacity: 0, offset: 1 },
  ], 'linear'),
  cue('.a3-call-underline', 2190, 1600, [
    { opacity: 0, transform: 'scaleX(.86)', offset: 0, easing: easeOut },
    { opacity: 1, transform: 'scaleX(.86)', offset: .17 },
    { opacity: 1, transform: 'scaleX(.86)', offset: .68, easing: cameraEase },
    { opacity: 1, transform: 'scaleX(1)', offset: 1 },
  ], 'linear'),
  cue('.a3-editor-baseline', 2490, 3080, [
    { opacity: 0, offset: 0, easing: easeOut },
    { opacity: 1, offset: .1 },
    { opacity: 1, offset: .78, easing: easeOut },
    { opacity: 0, offset: 1 },
  ], 'linear'),
  cue('.a3-accessor-old', 3150, 245, [
    { opacity: 1, transform: 'translateY(0)' }, { opacity: 0, transform: 'translateY(-7px)' },
  ]),
  fade('.a3-accessor-insert', 3285, 175, 0, 1),
  cue('.a3-accessor-new', 3400, 400, [
    { opacity: 0, transform: 'translateX(-4px)', clipPath: 'inset(0 90% 0 0)' },
    { opacity: 1, transform: 'translateX(0)', clipPath: 'inset(0)' },
  ]),
  cue('.a3-semicolon', 3335, 245, [
    { transform: 'translateX(0)' }, { transform: 'translateX(3ch)' },
  ]),
  fade('.a3-java-edit', 3460, 360, 0, 1),
  fade('.a3-edit-sign', 3680, 230, 0, 1),
  fade('.a3-propagation', 4730, 790, 1, 0),

  fade('.a3-checks', 4730, 640, 0, 1),
  fade('.a3-run-pending', 7670, 110, 1, 0),
  fade('.a3-waiting-number', 6850, 210, 1, 0),
  cue('.a3-retained-number', 6990, 490, [
    { opacity: 0, transform: 'translateY(6px)', clipPath: 'inset(0 44% 0 0)' },
    { opacity: 1, transform: 'translateY(0)', clipPath: 'inset(0)' },
  ]),
  fade('.a3-assertion-match', 7390, 330, 0, 1),
  fade('.a3-run-passed', 7750, 160, 0, 1),

  fade('.a3-publication', 9570, 500, 0, 1),
];
