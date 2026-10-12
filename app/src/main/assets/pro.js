'use strict';

function proAccess() {
  return !!state.pro?.access;
}
function showPro() {
  const pro = state.pro || {};
  open(
    `<h2>A little more possibility.</h2><p>My Health Pro is a one-time upgrade. Your daily steps, health journal, reminders, widget and JSON backups stay free.</p><div class="card"><h3>Unlock your Pro tools</h3><ul class="pro-features"><li>GPS walks, runs and rides with saved routes</li><li>Food barcode scanning and portion logging</li><li>30- and 90-day trends and movement calendars</li><li>Printable PDF health reports</li></ul><span class="badge">NO ADS · NO SUBSCRIPTION</span></div>${pro.preview ? '<div class="notice">Development preview: all Pro tools are available in this build.</div>' : ''}<p>${esc(pro.owned ? 'Pro is unlocked on your Google Play account' : pro.status || 'Available after the Google Play launch')}</p><button class="primary" data-action="buy-pro" ${pro.owned || !pro.configured || !pro.price ? 'disabled' : ''}>${pro.owned ? 'Pro unlocked' : pro.price ? 'Unlock Pro · ' + esc(pro.price) : 'Coming to Google Play'}</button><button class="secondary" data-action="restore-pro">Restore purchases</button><button class="link" data-action="close">Keep using My Health</button>`,
  );
}

const commercialMonthOverview = monthOverview;
monthOverview = function () {
  if (!proAccess() && reportDays > 7) reportDays = 7;
  return commercialMonthOverview();
};

document.addEventListener(
  'click',
  (event) => {
    const button = event.target.closest('button');
    if (!button) return;
    const gated =
      button.dataset.start ||
      ['camera', 'scan-food'].includes(button.dataset.action) ||
      button.dataset.report === 'pdf' ||
      +button.dataset.range > 7;
    if (gated && !proAccess()) {
      event.preventDefault();
      event.stopImmediatePropagation();
      showPro();
      return;
    }
    const action = button.dataset.action;
    if (action === 'pro') {
      showPro();
    } else if (action === 'buy-pro') {
      Health.buyPro();
    } else if (action === 'restore-pro') {
      Health.restorePro();
      toast(
        state.pro?.configured
          ? 'Checking your Google Play purchases…'
          : 'Purchases become available after the Google Play launch',
      );
    } else if (action === 'privacy') {
      Health.privacy();
    } else if (action === 'delete-health-data') {
      Health.deleteData();
    }
  },
  true,
);

const commercialSettings = settings;
settings = function () {
  return (
    commercialSettings() +
    `<div class="notice">My Health is a personal wellness journal, not a medical device. It does not diagnose, prevent, cure or treat medical conditions. Consult a qualified healthcare professional for medical advice.</div>`
  );
};
render();
