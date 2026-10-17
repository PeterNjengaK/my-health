'use strict';
const reviewBaseShowPro = showPro;
showPro = function () {
  reviewBaseShowPro();
  const panel = document.createElement('div');
  panel.id = 'pro-panel-state';
  panel.dataset.snapshot = JSON.stringify(state.pro || {});
  $('sheet').append(panel);
  if (state.pro?.reviewer && !state.pro?.owned) {
    const buy = $('sheet').querySelector('[data-action="buy-pro"]');
    buy.disabled = true;
    buy.textContent = 'Review access active';
    panel.innerHTML = '<p class="small muted">Full Pro features are available through a developer-issued review grant. This is not a Google Play purchase.</p>';
  }
  panel.insertAdjacentHTML('beforeend', '<button class="link" data-action="review-access">Review access</button>');
};
const reviewBaseRender = render;
render = function () {
  const panel = $('pro-panel-state');
  if (modal && panel && panel.dataset.snapshot !== JSON.stringify(state.pro || {})) {
    showPro();
    return;
  }
  reviewBaseRender();
};
document.addEventListener('click', event => {
  if (event.target.closest('button')?.dataset.action !== 'review-access') return;
  open('<h2>Review access</h2><p>For app review, enter the access code provided by the developer. It unlocks Pro tools without payment and does not create a Google Play purchase.</p><form id="review-access-form"><label for="review-code">Developer-issued access code</label><textarea id="review-code" name="code" maxlength="2048" rows="5" autocomplete="off" autocapitalize="off" spellcheck="false" required></textarea><button class="primary">Activate review access</button><button class="secondary" type="button" data-action="pro">Back to Pro</button></form>');
});
document.addEventListener('submit', event => {
  if (event.target.id !== 'review-access-form') return;
  event.preventDefault(); event.stopImmediatePropagation();
  const error = Health.reviewAccess(new FormData(event.target).get('code'));
  if (error) return toast(error);
  state = JSON.parse(Health.state());
  showPro();
  toast('Review access activated');
}, true);
